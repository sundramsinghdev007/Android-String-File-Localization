# Changelog

## [1.0.0] - 2026-03-08
### Added
- Initial release of Android String File Localization.
- **Automated Translation:** Support for Google Translate engine integration.
- **Smart UI:** Searchable string list with English previews.
- **Tools Menu Integration:** Access the plugin even when no files are open.
- **Selection Logic:** "Select All", "Deselect All", and "Force Update" functionality.
- **Sanitization:** Auto-escaping of apostrophes and removal of NBSP characters.
- **Internationalization:** Plugin UI ready for multiple languages via `MyBundle`.
- **Background Tasks:** Non-blocking translation with progress indicator.

### Fixed
- Fixed "Apostrophe not preceded by \" build errors.
- Resolved IDE stutters by moving update logic to Background Thread (BGT).