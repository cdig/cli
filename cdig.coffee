#!/usr/bin/env coffee

child_process = require "child_process"
fs = require "fs"
keytar = require "keytar"
path = require "path"
promptSync = require "prompt-sync"


basicHelp =
  "Tool Commands":
    help:                   "Display this helpful information"
    "help dev":             "Display advanced usage information"
    update:                 "Install the latest version of the cdig tool"
    "update all":           "Update brew, npm, cdig (may take 10 minutes)"
  "LBS Commands":
    auth:                   "View the current LBS API token"
    "auth TOKEN":           "Set the LBS API token"
  "Project Commands":
    new:                    "Set up a new project in this folder"
    pull:                   "Download files needed to run the project"
    clean:                  "Delete files generated by the tool\n"
    watch:                  "Serve a live-reloading build of the project\n"
    compile:                "Generate an optimized build of the project"
    push:                   "Upload the optimized build to our servers"
    register:               "Create an Artifact for the uploaded build\n"
    run:                    "clean + pull + watch"
    deploy:                 "compile + push + register"

devHelp =
  "Special Commands":
    scan:                   "Scans a lesson for translation issues"
    "screenshot PATH":      "Saves optimized PNGs to ~/Desktop. To dither, use --dither"
  Flags:
    "--gulp=PATH":          "Specify the gulpfile to use. Eg: --gulp=dev/cd-core/gulpfile.coffee"
    "--lbs=PATH":           "Specify the LBS url to use. Eg: --lbs=http://localhost:3000"


# Helpers #########################################################################################


# Who needs chalk when you can just roll your own ANSI escape sequences
do ()->
  fmts =
    bold: 1, dim: 2, italic: 3, underline: 4, overline: 53, inverse: 7, strike: 9,
    black: 30, red: 31, green: 32, yellow: 33, blue: 34, magenta: 35, cyan: 36, white: 37,
    blackBright: 90, grey: 90, redBright: 91, greenBright: 92, yellowBright: 93, blueBright: 94, magentaBright: 95, cyanBright: 96, whiteBright: 97,
    bgBlack: 40, bgRed: 41, bgGreen: 42, bgYellow: 43, bgBlue: 44, bgMagenta: 45, bgCyan: 46, bgWhite: 47,
    bgBlackBright: 100, bgGrey: 100, bgRedBright: 101, bgGreenBright: 102, bgYellowBright: 103, bgBlueBright: 104, bgMagentaBright: 105, bgCyanBright: 106, bgWhiteBright: 107
  for fmt, v of fmts
    do (fmt, v)-> global[fmt] = (t)-> "\x1b[#{v}m" + t + "\x1b[0m"

# Pad the start and end of a string to a target length
padAround = (str, len = 80, char = " ")->
  str.padStart(Math.ceil(len/2 + str.length/2), char).padEnd(len, char)

# Wrap a string to the a desired character length
# https://stackoverflow.com/a/51506718/313576
linewrap = (s, len = 80, sep = "\n")-> s.replace new RegExp("(?![^\\n]{1,#{len}}$)([^\\n]{1,#{len}})\\s", "g"), "$1#{sep}"

# Prepend a string with a newline. Useful when logging.
br = (m)-> "\n" + m

# Indent a string using a given line prefix
indent = (str, prefix = "  ")-> prefix + str.replaceAll "\n", "\n" + prefix

# Surround a string with another string
surround = (inner, outer = " ")-> outer + inner + outer

# Generate a nice divider, optionally with text in the middle
divider = (s = "")-> br padAround(s, 80, "─") + "\n"

# console.log should have expression semantics
log = (...args)-> console.log ...args; args[0]

# Saner default for execSync
exec = (cmd, opts = {stdio: "inherit"})-> child_process.execSync cmd, opts

# Little sugary filesystem helpers
exists = (filePath)-> fs.existsSync filePath
readdir = (filePath)-> fs.readdirSync(filePath).filter (i)-> i isnt ".DS_Store"
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
  log yellow question
  for k, v of answers
    log "Enter #{cyan k} for #{v}"
  answer = promptSync(sigint:true) "Answer: "
  if answers[answer]
    answers[answer]
  else
    log red "... Wasn't expecting that answer."

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
      if hint? then log red "Cannot #{command} — please make sure you're in the correct folder, and run " + yellow "cdig #{hint} " + red "if needed"
      if msg? then log msg
    return hasFiles


# Commands ########################################################################################

commands = {}


# Tool Commands

printHelp = (helpItems)->
  maxNameLength = 0
  for label, group of helpItems
    for name, description of group
      maxNameLength = Math.max name.length, maxNameLength

  for label, group of helpItems
    log ""
    log green "  " + label
    log ""
    for name, description of group
      log yellow "    " + name.padEnd(maxNameLength + 2) + blue description


commands.help = (mode)->
  log ""
  log cyan "  The CDIG Tool • Version #{version()}"

  if mode isnt "dev"
    log ""
    log "  You can run any of the commands listed below."
    log "  For example, run " + yellow("cdig help") + " to see this info."
    printHelp basicHelp
  else
    printHelp devHelp

  log ""

commands.update = (mode)->
  if mode is "all"
    log yellow "\nUpdating " + cyan "brew " + yellow "packages...\n"
    exec "brew update"
    exec "brew upgrade"
    exec "brew cleanup"
    log yellow "\nUpdating " + cyan "npm " + yellow "packages...\n"
    exec "npm i -g npm --silent"
    exec "npm i -g coffeescript gulp-cli --silent"
  log yellow "\nUpdating " + cyan "cdig " + yellow "package...\n"
  exec "npm i -g cdig --silent"

  log green "Your CDIG Tool is now version:"
  exec "cdig version"
  log ""

commands.version = ()->
  log version()

# LBS Commands

# Get/set the LBS API token for this user
commands.auth = (token)->
  if token?
    keytar.setPassword "com.lunchboxsessions.cli", "api-token", token
    log green "Your API token has been saved"
  else
    token = await keytar.getPassword "com.lunchboxsessions.cli", "api-token"
    log yellow "Your current token is: " + blue token


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
  gulpfile = flags.gulp or "node_modules/cd-core/gulpfile.coffee"
  exec "gulp --gulpfile #{gulpfile} --cwd . #{cmd}"

last = (arr)-> arr[arr.length-1]

projectName = ()->
  last process.cwd().split(path.sep)

indexName = ()->
  if isDir "deploy"
    path.basename readdir("deploy/index")[0]

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
    log red "This folder already contains a project"
  else
    if type = getProjectType()
      log yellow "Creating a new #{type} project..."
      typeFiles = newProjectFiles[type]
      mkdir "resources"
      pullFromOrigin type, typeFiles

commands.pull = ()->
  return unless commandHasNeededFiles command: "watch", files: "source", hint: "new"
  log yellow "Downloading system files and dependencies..."
  type = getProjectType()
  rm file for file in systemFiles
  pullFromOrigin type, systemFiles
  pullNodeModules() # TODO: split this out into its own command, so that I can run it separately when debugging dependency stuff

commands.clean = ()->
  return unless commandHasNeededFiles command: "clean", files: "source", msg: red "Cleaning cancelled — this doesn't look like a cdig project. Make sure you're in the correct folder."
  log yellow "Cleaning..."
  rm file for file in systemFiles
  rm file for file in generatedFiles


commands.watch = ()->
  return unless commandHasNeededFiles command: "watch", files: "node_modules", hint: "pull"
  log yellow "Watching... (press control-c to stop)"
  gulp getProjectType() + ":dev"


commands.compile = ()->
  return unless commandHasNeededFiles command: "compile", files: "node_modules", hint: "pull"
  log yellow "Compiling deployable build..."
  gulp getProjectType() + ":prod"

commands.push = ()->
  return unless commandHasNeededFiles command: "push", files: "deploy", hint: "compile"
  log yellow "Pushing to S3..."
  exec "aws s3 sync deploy/all s3://lbs-cdn/#{era}/ --size-only --exclude \".*\" --cache-control max-age=31536000,immutable"

commands.register = ()->
  return unless commandHasNeededFiles command: "register", files: "deploy", hint: "compile"
  log yellow "Registering with LBS..."
  domain = flags.lbs or "https://www.lunchboxsessions.com"
  res = await post domain + "/cli/artifacts",
    era: era
    name: projectName()
    source: indexName()
    head: indexHead()
    body: indexBody()
  text = await res.text()
  log yellow text
  if isUrl text
    exec "open #{text}"
  else
    log red text


commands.run = ()->
  do commands[c] for c in ["clean", "pull", "watch"]

commands.deploy = ()->
  do commands[c] for c in ["compile", "push", "register"]


# Dev Commands ####################################################################################

commands.screenshot = (path)->
  flag = if flags.dither then "" else "--nofs"
  for i in [16, 20, 24, 32, 40, 48, 64, 80, 96, 128, 192, 256]
    exec "pngquant #{flag} --output ~/Desktop/#{i}.png #{i} #{path}"
  null

commands.scan = ()->
  pagesPath = "source/pages/"
  enPagesPath = "source/pages-en/"

  exec "clear"

  # Print the name of the lesson folder
  name = path.basename process.cwd()
  log br(padAround(surround(name), 80, "◜◝◟◞")).replace(name, magenta name).replaceAll /([◜◝◟◞])/g, cyan "$1"
  log ""

  # Bail if this lesson hasn't been translated
  if not exists enPagesPath
    log linewrap "Easy! This lesson hasn't been translated to Spanish. Please return it to the Dropbox and proceed to the next lesson."
    log divider()
    return

  # Print the instructions
  instructions = "For each diff below, edit the HTML in pages-en and pages-es to match pages."
  log padAround(instructions).replaceAll(/(pages(-en|-es)*)/g, yellow "$1")
  log ""
  log green "                        Current English: " + yellow "source/pages"
  log red   "                            Old English: " + yellow "source/pages-en"
  log       "                                Spanish: " + yellow "source/pages-es"
  log "\n"

  pages = readdir enPagesPath
  hasDiff = false

  for page, i in pages
    log cyan divider surround "#{page} (#{i+1}/#{pages.length})"

    # Compute the diff
    old = surround enPagesPath + page, '"'
    nue = surround pagesPath + page, '"'
    diff = exec "diff -u #{old} #{nue} | diff-so-fancy", encoding: "utf-8"

    # We've got a diff!
    if diff.length
      hasDiff = true

      # Split the diff into lines, and ignore some noise at the beginning
      lines = diff.split("\n")[3..]

      for line, j in lines
        # Some lines just show a path where a change occurs — replace those with an ellipsis
        if line.indexOf(pagesPath + page) > 0
          log br(dim padAround "...") + "\n" if j > 0
        else
          log line

    # No diff!
    else
      log padAround "Old and new versions of this page are identical. Nice!" + "\n"

  log cyan divider()

  if hasDiff
    log cyan padAround "Analysis complete. Scroll up to the top!"
  else
    log green "The old and new English versions are identical. Please return this lesson to the Dropbox and proceed to the next lesson."

  log cyan divider()


# Main ############################################################################################

args = process.argv[2..]
flags = {}

args = args.filter (arg)->
  isFlag = arg.startsWith "-"
  if isFlag
    [k, v] = arg.split "="
    k = k.replace /-+/, "" # strip leading dashes
    flags[k] = v
  !isFlag

command = args.shift() or "help"

if c = commands[command]
  c ...args
else
  log red "\n  Error: " + yellow command + red " is not a valid command."
  commands.help()
