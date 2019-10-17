Drawing Module
=======
This module provides a way to draw and annotate on images and save them as complex observation. You can find more information at https://wiki.openmrs.org/display/docs/Drawing+Module 

## Additional Installation Steps
1. Download Drawing module from here and upload it into your system .
1. Create a new concept (You can do this by clicking on Dictionary and Add new Concept)
1. If there is a class 'Drawing' in the list , set the class of concept to 'Drawing'. if not create a new class  'Drawing' and set the class field of the concept to the same(you can create new class from administration ->Manage Concept Classes )
1. Set the data type to complex
1. Set the handler to DrawingHandler

[User Documentation](https://wiki.openmrs.org/display/docs/Drawing+Module+-+User+Documentation)

TL;DR
`<drawing id="drawingEditor" conceptId="" displayMode="annotation" width="500px" height="250px" preloadResImage="web/module/resources/images/DrawingTemplates/Full Body Lateral Female.png">`

## Breaking Changes
questionConceptId has been changed to conceptId

## Required tag attributes as 2.0.0

An id attribute (e.g. `<drawing id="drawingEditor"/>`) is still required.

`conceptId` is required as with many HFE tags.

`displayMode` is a new and required attribute.

### Display Modes
The Drawing module now explicity provides two display modes, one specifically for drawing and annotating images, and one specifically for use with signatures.

`<drawing displayMode="{annotation, signature}" conceptId="(HFE sufficient Conecpt identifier)"/>`

## New attributes supported in 2.0.0

Height and width
`<drawing displayMode="annotation" conceptId="(complexId)" width="500px" height="250px">`

Preloaded "template" Image (from drawing or module internal resources)
`<drawing displayMode="annotation" conceptId="(complexId)" preloadResImage="/web/module/resources/images/example.png"/>`

