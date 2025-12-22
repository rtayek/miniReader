Fixed the reported compile errors:

Made CoreRuntime public so CoreFacade can reference it, while keeping it in the internal package.
Removed the stray looksLikeJsShell method and the unused JS_SHELL_MAX_LENGTH constant; JS-shell detection remains via ShellHeuristics.
Cleaned up CoreRuntime accordingly.