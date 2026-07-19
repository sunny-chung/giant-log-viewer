# Giant Log Viewer Changelog

All notable changes to the Giant Log Viewer software will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

_Changes since 1.2.0_

### Added
- Follow new appends to the end of the opened file (Shift-F)
- Support UTF-8 with BOM and UTF-16 encodings in additional to current encoding support of UTF-8 without BOM
- Long lines (>= 1 MB per line) are now supported
- Status bar, which shows current byte range, last modified date time and current encoding
- Shift-Click to extend selection
- Dragging out of view to scroll and extend selection
- Copy selection(s) to a file
- Option to turn off soft wrapping
- Column / Vertical selection

### Changed
- New appends to the opened file are not automatically followed anymore
- After going pass the last search result, searching reversely would first visit the last search result
- Copy text length limit is increased from 1 MB to 5 MB
- Improved error display for regular expression searching

### Fixed
- Mouse scrolling did not reposition the search cursor correctly
- Search cursor was incorrect after going pass the last search result
- Search result states did not reset after loading a new file
- Possible overflow when a file with over 1 TB size is navigated


## [1.2.0] -- 2025-05-12

Now giant text files can be navigated by mouses and trackpads in additional to keyboards.

_Changes since 1.1.1_

### Added
- Scroll to navigate
- Click or drag the vertical bar to navigate
- Search bar text field background now changes according to the search result

### Fixed
- Crash while reading a 7 GB file (#3)


## [1.1.1] -- 2025-04-18

_Changes since 1.1.0_

### Fixed
- Crash at startup on Windows (#1)
- Width of the About window was not long enough for low screen density PCs
- Title text color of the Help window did not change according to color theme


## [1.1.0] -- 2025-04-16

### Added
- Dark color theme
- Persistence -- to save your preference if you have changed any setting

### Fixed
- Cursor selection was incorrect

### Optimized
- Searching text field rendering performance


## [1.0.1] -- 2025-04-10

### Optimized
- Rendering performance


## [1.0.0] -- 2025-04-08

Project launch! 🎉
