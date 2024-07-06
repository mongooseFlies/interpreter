## Dynamic Programming Language 

tiny dynamically-typed programming language 

> [!WARNING]
> Personal Project - Not ready for production

### Features

* Dynamic Typing: Variables do not require explicit type declaration.
* Interactive Shell: Execute code line-by-line for testing and exploration.
* Execute a source file.

### Getting Started

1. **Prerequisites:** 

    * Kotlin ([https://kotlinlang.org/](https://kotlinlang.org/))

### Usage

* Use the Repl
* Run an entire source file

### **Example:**
see demo file [here](demo.txt)

## TODO list

* [x] Control flow
* [x] Functions with parameters
* [x] Closures
* [x] Classes
* [x] Methods
* [ ] Inheritance

## Grammar

    expression |> literal    |
                  binary     |
                  unary      |
                  grouping   |

    literal   -> NUMBER | STRING | 'false' | 'true' | 'nil'
    binary    -> expression operator expression
    unary     -> operator expression
    grouping  -> '('  expression ')'
    operator  -> '==' | '>=' | '<' | '<=' | '+' | '-' | '*' | '/' | '!=' |

    expression => or
    or         => and
    and        => equality
    equality   => comparison ('==' | '!=') comparison
    comparison => term ('+' | '-') term
    term       => factor ('*' | '/') factor
    factor     => primary
    primary    => STRING | NUMBER | 'false' | 'true' | nil | grouping
    grouping     => '(' expression ')'
