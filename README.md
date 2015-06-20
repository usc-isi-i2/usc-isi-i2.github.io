# [usc-isi-i2.github.io](http://usc-isi-i2.github.io)
USC ISI information integration group website

## Folder Structure
- Under root
  - `papers` put all papers here
  - `slides` put all slides here
  - `projects` put old or outdated projects here
- Under a person folder, such as `knoblock`
  - `doc` put your bibtex file and other related documents here. To be consistent, we recommend you name your bibtex of all publications `complete.bib` and bibtex of selected publications to show on your page `selected.bib`

## Change CSS
The css files in folder `css` are compiled from the scss files in folder `scss`. **Don't** change the css directly. Check out [Technology Stack](#technology-stack) to see how to run the compilation automatically.

## How Word Cloud Works
I use the one in knoblock's page as an example.
HTML:
```html
<div id="word-cloud-container" data-bib="doc/complete.bib" data-min-word-count="1" data-max-word-count="70" data-min-font-size="2" data-max-font-size="40"></div>
```
- `id="word-cloud-container"` id is used in `word-cloud.js` to locate it.
- `data-bib` path to your bibtex file which is used to generate word cloud from paper titles.
- (`data-min-word-count`, `data-max-word-count`) and (`data-min-font-size`, `data-max-font-size`) define a range map from the word count to its size in the cloud. You need to adjust these parameters to make the word cloud look nice.

## Technology Stack
Please get yourself familiar with the following stuff at first. It will make your life easier.
- (required) Working knoledge of Git and Unix shell
- (required) HTML, JavaScript, Bootstrap
- (required) Sass
- To automate workflow: [Node.js](http://nodejs.org/), `npm`, [Grunt](http://gruntjs.com/)

## Local Set Up
1. Make sure you're on Mac OS or *nix!
2. Install [Node.js](http://nodejs.org/)
3. Install [Grunt](http://gruntjs.com/), `npm install -g grunt-cli`
4. Download the repository and go to its directory, type `npm install`
