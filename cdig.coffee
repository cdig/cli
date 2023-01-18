#!/usr/bin/env coffee

child_process = require "child_process"
fs = require "fs"
keytar = require "keytar"
path = require "path"
promptSync = require "prompt-sync"


descriptions =
  Tool:
    help:             "Display this helpful information"
    "help dev":       "Display advanced usage information"
    update:           "Install the latest version of the cdig tool"
    "update all":     "Update brew, npm, cdig (may take 10 minutes)"
  LBS:
    auth:             "View the current LBS API token"
    "auth TOKEN":     "Set the LBS API token"
  Project:
    new:              "Set up a new project in this folder"
    pull:             "Download files needed to run the project"
    clean:            "Delete files generated by the tool\n"
    watch:            "Serve a live-reloading build of the project\n"
    compile:          "Generate an optimized build of the project"
    push:             "Upload the optimized build to our servers"
    register:         "Create an Artifact for the uploaded build\n"
    run:              "clean + pull + watch"
    deploy:           "compile + push + register"
  Dev:
    "register local": "For testing deployment, register with LBS at localhost:3000"
    "screenshot FLAG PATH": "Produce optimized PNGs. FLAG must be --dither or --no-dither"


# Helpers #########################################################################################


# Who needs chalk when you can just roll your own ANSI escape sequences
do ()->
  global.white = (t)-> t
  for color, n of red: "31", green: "32", yellow: "33", blue: "34", magenta: "35", cyan: "36"
    do (color, n)-> global[color] = (t)-> "\x1b[#{n}m" + t + "\x1b[0m"

# Saner default for execSync
exec = (cmd)-> child_process.execSync cmd, stdio: "inherit"

# Little sugary filesystem helpers
exists = (filePath)-> fs.existsSync filePath
readdir = (filePath)-> fs.readdirSync filePath
mkdir = (filePath)-> fs.mkdirSync filePath, recursive: true
rm = (filePath)-> if exists filePath then fs.rmSync filePath, recursive: true
isDir = (filePath)-> fs.statSync(filePath).isDirectory()
read = (filePath)-> if exists filePath then fs.readFileSync(filePath).toString()

version = ()-> require("./package.json").version

post = (url, data)->
  token = await keytar.getPassword "com.lunchboxsessions.cli", "api-token"
  await fetch url,
    method: "POST"
    headers:
      "Content-Type": "application/json"
      "X-LBS-API-TOKEN": token
    body: JSON.stringify data

download = (url, filePath)->
  exec "curl --create-dirs -fsSo #{filePath} #{url}"

isUrl = (str)->
  str.startsWith("http://") or str.startsWith("https://")

prompt = (question, answers)->
  console.log yellow question
  for k, v of answers
    console.log "Enter #{k} for #{v}"
  answer = promptSync(sigint:true) "Answer: "
  if answers[answer]
    answers[answer]
  else
    console.log red "... Wasn't expecting that answer."

# Get or prompt for the project type. This function is stateful to avoid redundant prompts.
projectType = null
getProjectType = ()->
  projectType = if projectType
    projectType
  else if t = read("cdig.json")?.type
    t
  else if exists "source/config.coffee"
    "svga"
  else if exists "source/index.kit"
    "cd-module"
  else
    prompt "What type of project is this?", {m: "cd-module", s: "svga"}

commandHasNeededFiles = ({command, files, hint, msg})->
    hasFiles = exists files
    if not hasFiles
      if hint? then console.log red "Cannot #{command} — please make sure you're in the correct folder, and run " + yellow "cdig #{hint} " + red "if needed"
      if msg? then console.log msg
    return hasFiles


# Commands ########################################################################################

commands = {}


# Tool Commands

commands.help = (mode)->
  console.log ""
  console.log cyan "  The CDIG Tool • Version #{version()}"

  inDevMode = mode is "dev"

  if not inDevMode
    console.log ""
    console.log "  You can run any of the commands listed below."
    console.log "  For example, run " + yellow("cdig help") + " to see this info."

  maxNameLength = 0
  for label, group of descriptions when (label is "Dev") is inDevMode
    for name, description of group
      maxNameLength = Math.max name.length, maxNameLength

  for label, group of descriptions when (label is "Dev") is inDevMode
    console.log ""
    console.log green "  " + label + " Commands:"
    console.log ""
    for name, description of group
      console.log yellow "    " + name.padEnd(maxNameLength + 2) + blue description
  console.log ""

commands.update = (mode)->
  if mode is "all"
    console.log yellow "\nUpdating " + cyan "brew " + yellow "packages...\n"
    exec "brew update"
    exec "brew upgrade"
    exec "brew cleanup"
    console.log yellow "\nUpdating " + cyan "npm " + yellow "packages...\n"
    exec "npm i -g npm --silent"
    exec "npm i -g coffeescript gulp-cli --silent"
  console.log yellow "\nUpdating " + cyan "cdig " + yellow "package...\n"
  exec "npm i -g cdig --silent"

  console.log green "Your CDIG Tool is now version:"
  exec "cdig version"
  console.log ""

commands.version = ()->
  console.log version()

# LBS Commands

# Get/set the LBS API token for this user
commands.auth = (token)->
  if token?
    keytar.setPassword "com.lunchboxsessions.cli", "api-token", token
    console.log green "Your API token has been saved"
  else
    token = await keytar.getPassword "com.lunchboxsessions.cli", "api-token"
    console.log yellow "Your current token is: " + blue token


# Project Commands

era = "v4-1" # TODO — make this dynamic
systemFiles = [".gitignore", "cdig.json", "package.json"]
generatedFiles = [".DS_Store", ".git", "deploy", "gulpfile.coffee", "node_modules", "package-lock.json", "public", "yarn.lock", "yarn-error.log"]
newProjectFiles =
  "cd-module": ["source/index.kit", "source/pages/objectives.html"]
  svga: ["source/root.coffee", "source/config.coffee"]

pullFromOrigin = (type, files)->
  baseUrl = "https://raw.githubusercontent.com/cdig/#{type}-starter/#{era}/dist/"
  for file in files
    download baseUrl + file, file

pullNodeModules = ()->
  if exists "~/cdig/cli/node_modules"
    exec "cp -a ~/cdig/cli/node_modules ."
  else
    exec "npm update --silent"

gulp = (cmd)->
  exec "gulp --gulpfile node_modules/cd-core/gulpfile.coffee --cwd . #{cmd}"

last = (arr)-> arr[arr.length-1]

projectName = ()->
  last process.cwd().split(path.sep)

indexName = ()->
  if isDir "deploy"
    path.basename fs.readdirSync("deploy/index")[0]

indexFragment = (fragmentName)->
  file = read "deploy/all/#{indexName()}"
  beginMarker = "<!--! begin " + fragmentName + " -->"
  endMarker = "<!--! end " + fragmentName + " -->"
  beginIndex = file.indexOf(beginMarker) + beginMarker.length
  endIndex = file.indexOf endMarker
  file[beginIndex...endIndex]

indexHead = ()-> indexFragment "head"
indexBody = ()-> indexFragment "body"


commands.new = ()->
  if exists "source"
    console.log red "This folder already contains a project"
  else
    if type = getProjectType()
      console.log yellow "Creating a new #{type} project..."
      typeFiles = newProjectFiles[type]
      mkdir "resources"
      pullFromOrigin type, typeFiles

commands.pull = ()->
  return unless commandHasNeededFiles command: "watch", files: "source", hint: "new"
  console.log yellow "Downloading system files and dependencies..."
  type = getProjectType()
  rm file for file in systemFiles
  pullFromOrigin type, systemFiles
  pullNodeModules() # TODO: split this out into its own command, so that I can run it separately when debugging dependency stuff

commands.clean = ()->
  return unless commandHasNeededFiles command: "clean", files: "source", msg: red "Cleaning cancelled — this doesn't look like a cdig project. Make sure you're in the correct folder."
  console.log yellow "Cleaning..."
  rm file for file in systemFiles
  rm file for file in generatedFiles


commands.watch = ()->
  return unless commandHasNeededFiles command: "watch", files: "node_modules", hint: "pull"
  console.log yellow "Watching... (press control-c to stop)"
  gulp getProjectType() + ":dev"


commands.compile = ()->
  return unless commandHasNeededFiles command: "compile", files: "node_modules", hint: "pull"
  console.log yellow "Compiling deployable build..."
  gulp getProjectType() + ":prod"

commands.push = ()->
  return unless commandHasNeededFiles command: "push", files: "deploy", hint: "compile"
  console.log yellow "Pushing to S3..."
  exec "aws s3 sync deploy/all s3://lbs-cdn/#{era}/ --size-only --exclude \".*\" --cache-control max-age=31536000,immutable"

commands.register = (mode)->
  return unless commandHasNeededFiles command: "register", files: "deploy", hint: "compile"
  console.log yellow "Registering with LBS..."
  domain = if mode is "local"
    "http://localhost:3000"
  else
    "https://www.lunchboxsessions.com"
  res = await post domain + "/cli/artifacts",
    era: era
    name: projectName()
    source: indexName()
    head: indexHead()
    body: indexBody()
  text = await res.text()
  console.log yellow text
  if isUrl text
    exec "open #{text}"
  else
    console.log red text


commands.run = ()->
  do commands[c] for c in ["clean", "pull", "watch"]

commands.deploy = ()->
  do commands[c] for c in ["compile", "push", "register"]


# Dev Commands ####################################################################################

commands.screenshot = (ditherFlag, path)->
  flag = if ditherFlag is "--dither" then "" else "--nofs"
  for i in [16, 20, 24, 32, 40, 48, 64, 80, 96, 128, 192, 256]
    exec "pngquant #{flag} --output ~/Desktop/#{i}.png #{i} #{path}"
  null


# Main ############################################################################################

args = process.argv[2..]

command = args.shift()
command ?= "help"
command = command.replace "--", ""

if c = commands[command]
  c ...args
else
  console.log red "\n  Error: " + yellow command + red " is not a valid command."
  commands.help()
