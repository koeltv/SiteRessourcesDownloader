# Command line utility to download ressources from a given url

This program is used to download ressources from a given url. It is used via command line like follows:

## Installation

1. Install the utility via one of the installation options
2. Install it or extract from the archive
3. Run the program

## How to use

To download the ressources from a given url, just paste the url in the opened terminal and press enter.
If the url correspond to a known URL pattern, all the ressources will be downloaded in your download folder.

## How does it work

This program uses [selenium](https://www.selenium.dev/) to open a browser and download the ressources from the given url. It is able to download the ressources from the following websites:
- padlet.com

This list can be expended by adding a new `Scraper` class implementing `SiteScraper`

It uses the firefox driver to open the browser. It should be installed automatically when the program is launched or use any already existing installation if it is already installed.


Icon by [Freepik - Flaticon](https://www.flaticon.com/free-icons/download)