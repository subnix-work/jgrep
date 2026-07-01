package de.subnix.ygrep;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.Locale;
import java.util.concurrent.Callable;

@Command(
    name = "completion",
    mixinStandardHelpOptions = true,
    description = "Generate shell completion script"
)
public class CompletionCommand implements Callable<Integer>
{
    @Parameters(index = "0", paramLabel = "SHELL",
            description = "Shell to generate completion for: bash, zsh, fish, powershell")
    String shell;

    @Override
    public Integer call()
    {
        switch (shell.toLowerCase(Locale.ROOT))
        {
            case "bash" -> System.out.print(bash());
            case "zsh" -> System.out.print(zsh());
            case "fish" -> System.out.print(fish());
            case "powershell", "pwsh" -> System.out.print(powershell());
            default ->
            {
                System.err.println("ygrep: unsupported shell: " + shell);
                System.err.println("Supported shells: bash, zsh, fish, powershell");
                return 2;
            }
        }
        return 0;
    }

    private String bash()
    {
        return """
                # bash completion for ygrep
                _ygrep_completion() {
                  local cur prev opts shells
                  COMPREPLY=()
                  cur="${COMP_WORDS[COMP_CWORD]}"
                  prev="${COMP_WORDS[$((COMP_CWORD - 1))]}"
                  opts="-r --recursive -l --files-with-matches -c --count -s --slurp -n --null-input -f --from-file --pretty --no-color --color-level --color-level-field -h --help -V --version completion"
                  shells="bash zsh fish powershell"

                  if [[ "${COMP_WORDS[1]}" == "completion" ]]; then
                    COMPREPLY=( $(compgen -W "$shells" -- "$cur") )
                    return 0
                  fi

                  if [[ "$COMP_CWORD" -eq 1 && "$cur" != -* ]]; then
                    COMPREPLY=( $(compgen -W "completion" -- "$cur") $(compgen -f -- "$cur") )
                    return 0
                  fi

                  if [[ "$prev" == "--color-level-field" ]]; then
                    COMPREPLY=()
                    return 0
                  fi

                  if [[ "$cur" == -* ]]; then
                    COMPREPLY=( $(compgen -W "$opts" -- "$cur") )
                    return 0
                  fi

                  COMPREPLY=( $(compgen -f -- "$cur") )
                }
                complete -F _ygrep_completion ygrep
                """;
    }

    private String zsh()
    {
        return """
                #compdef ygrep
                # zsh completion for ygrep
                _ygrep() {
                  local context state line
                  typeset -A opt_args

                  if [[ "${words[2]}" == "completion" ]]; then
                    _arguments '2:shell:(bash zsh fish powershell)'
                    return
                  fi

                  _arguments -C \\
                    '(-r --recursive)'{-r,--recursive}'[Recurse into directories]' \\
                    '(-l --files-with-matches)'{-l,--files-with-matches}'[Only print filenames with matches]' \\
                    '(-c --count)'{-c,--count}'[Print match count per file]' \\
                    '(-s --slurp)'{-s,--slurp}'[Collect all results into a single JSON array]' \\
                    '(-n --null-input)'{-n,--null-input}'[Use null as input instead of reading files]' \\
                    '(-f --from-file)'{-f,--from-file}'[Read filter expression from a file]:filter file:_files' \\
                    '--pretty[Pretty-print JSON output]' \\
                    '--no-color[Disable colored output]' \\
                    '--color-level[Color each output line by log level]' \\
                    '--color-level-field[Field used by --color-level]:field:' \\
                    '(-h --help)'{-h,--help}'[Show help message]' \\
                    '(-V --version)'{-V,--version}'[Print version]' \\
                    '1:filter or command:(completion)' \\
                    '*:files:_files'
                }
                _ygrep "$@"
                """;
    }

    private String fish()
    {
        return """
                # fish completion for ygrep
                complete -c ygrep -s r -l recursive -d 'Recurse into directories'
                complete -c ygrep -s l -l files-with-matches -d 'Only print filenames with matches'
                complete -c ygrep -s c -l count -d 'Print match count per file'
                complete -c ygrep -s s -l slurp -d 'Collect all results into a single JSON array'
                complete -c ygrep -s n -l null-input -d 'Use null as input instead of reading files'
                complete -c ygrep -s f -l from-file -r -d 'Read filter expression from a file'
                complete -c ygrep -l pretty -d 'Pretty-print JSON output'
                complete -c ygrep -l no-color -d 'Disable colored output'
                complete -c ygrep -l color-level -d 'Color each output line by log level'
                complete -c ygrep -l color-level-field -r -d 'Field used by --color-level'
                complete -c ygrep -s h -l help -d 'Show help message'
                complete -c ygrep -s V -l version -d 'Print version'
                complete -c ygrep -f -n 'not __fish_seen_subcommand_from completion' -a completion -d 'Generate shell completion script'
                complete -c ygrep -f -n '__fish_seen_subcommand_from completion' -a 'bash zsh fish powershell'
                """;
    }

    private String powershell()
    {
        return """
                # PowerShell completion for ygrep
                Register-ArgumentCompleter -Native -CommandName ygrep -ScriptBlock {
                  param($wordToComplete, $commandAst, $cursorPosition)

                  $elements = $commandAst.CommandElements | ForEach-Object { $_.Extent.Text }
                  if ($elements.Count -ge 2 -and $elements[1] -eq 'completion') {
                    @('bash', 'zsh', 'fish', 'powershell') |
                      Where-Object { $_ -like "$wordToComplete*" } |
                      ForEach-Object { [System.Management.Automation.CompletionResult]::new($_, $_, 'ParameterValue', $_) }
                    return
                  }

                  @('-r', '--recursive', '-l', '--files-with-matches', '-c', '--count',
                    '-s', '--slurp', '-n', '--null-input', '-f', '--from-file',
                    '--pretty', '--no-color', '--color-level',
                    '--color-level-field', '-h', '--help', '-V', '--version', 'completion') |
                    Where-Object { $_ -like "$wordToComplete*" } |
                    ForEach-Object { [System.Management.Automation.CompletionResult]::new($_, $_, 'ParameterName', $_) }
                }
                """;
    }
}
