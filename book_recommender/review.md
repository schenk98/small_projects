## Code review
### code breaking bugs
error_bad_lines=False -> on_bad_lines='skip'
(error_bad_lines is deprecated/removed in newer pandas)

String normalization fix for matching
Lowercasing step wasn’t reliably applied for runtime string dtype, so exact title/author match returned zero rows.

Groupby mean fix
.mean() was being applied in a way that included string data; changed to average only Book-Rating.

Added result print
Obtained result was never returned nor printed.

### Code cleaning
added logger and logs
refactored code into functions


### pros & cons
#### Pros
Code was consise, small and idea behind the algorithm was clear
Code was well commented and lightweight
Good enough for quick PoC
#### Cons
- Code structure was too complex and hard to read and debug - separate code into classes or at least functions.
- There were some code breaking problems documented above
- Short readme or documentation is missing
- requirements.txt is missing
- Magical constants in code, like book rating threshold - at least move them as constants to the top of the file or better to standalone constants file
- Variable names are too specific for given proof of concept and need to be changed for program (including strings)
