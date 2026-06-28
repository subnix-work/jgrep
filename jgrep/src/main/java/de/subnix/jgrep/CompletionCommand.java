package de.subnix.jgrep;

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
                System.err.println("jgrep: unsupported shell: " + shell);
                System.err.println("Supported shells: bash, zsh, fish, powershell");
                return 2;
            }
        }
        return 0;
    }

    private String bash()
    {
        return """
                # bash completion for jgrep
                _jgrep_completion() {
                  local cur prev opts shells
                  COMPREPLY=()
                  cur="${COMP_WORDS[COMP_CWORD]}"
                  prev="${COMP_WORDS[$((COMP_CWORD - 1))]}"
                  opts="-r --recursive -l --files-with-matches -c --count -s --slurp -n --null-input -f --from-file --pretty --yaml --no-color --color-level --color-level-field -h --help -V --version completion"
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
                complete -F _jgrep_completion jgrep
                """;
    }

    private String zsh()
    {
        return """
                #compdef jgrep
                # zsh completion for jgrep
                _jgrep() {
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
                    '--yaml[Read input as YAML]' \\
                    '--no-color[Disable colored output]' \\
                    '--color-level[Color each output line by log level]' \\
                    '--color-level-field[Field used by --color-level]:field:' \\
                    '(-h --help)'{-h,--help}'[Show help message]' \\
                    '(-V --version)'{-V,--version}'[Print version]' \\
                    '1:filter or command:(completion)' \\
                    '*:files:_files'
                }
                _jgrep "$@"
                """;
    }

    private String fish()
    {
        return """
                # fish completion for jgrep
                complete -c jgrep -s r -l recursive -d 'Recurse into directories'
                complete -c jgrep -s l -l files-with-matches -d 'Only print filenames with matches'
                complete -c jgrep -s c -l count -d 'Print match count per file'
                complete -c jgrep -s s -l slurp -d 'Collect all results into a single JSON array'
                complete -c jgrep -s n -l null-input -d 'Use null as input instead of reading files'
                complete -c jgrep -s f -l from-file -r -d 'Read filter expression from a file'
                complete -c jgrep -l pretty -d 'Pretty-print JSON output'
                complete -c jgrep -l yaml -d 'Read input as YAML'
                complete -c jgrep -l no-color -d 'Disable colored output'
                complete -c jgrep -l color-level -d 'Color each output line by log level'
                complete -c jgrep -l color-level-field -r -d 'Field used by --color-level'
                complete -c jgrep -s h -l help -d 'Show help message'
                complete -c jgrep -s V -l version -d 'Print version'
                complete -c jgrep -f -n 'not __fish_seen_subcommand_from completion' -a completion -d 'Generate shell completion script'
                complete -c jgrep -f -n '__fish_seen_subcommand_from completion' -a 'bash zsh fish powershell'
                """;
    }

    private String powershell()
    {
        return """
                # PowerShell completion for jgrep
                Register-ArgumentCompleter -Native -CommandName jgrep -ScriptBlock {
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
                    '--pretty', '--yaml', '--no-color', '--color-level',
                    '--color-level-field', '-h', '--help', '-V', '--version', 'completion') |
                    Where-Object { $_ -like "$wordToComplete*" } |
                    ForEach-Object { [System.Management.Automation.CompletionResult]::new($_, $_, 'ParameterName', $_) }
                }
                """;
    }
}
