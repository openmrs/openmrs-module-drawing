var drawing;
var tool;

var toolMap = {
  "draw" : {},
  "layer" : {}
};

var scrollbarWidth = 0;
var scrollbarHeight = 0;

function calcScrollbarsizes(){
	//add scrollbar overflow element to document
	
	var divElem = document.createElement("div");
	divElem.style.overflow = "scroll";
	divElem.style.width = "100px";
	divElem.style.height = "100px";
	document.body.append(divElem);
	
	//take difference of offset and client
	scrollbarWidth = divElem.offsetWidth - divElem.clientWidth;
	scrollbarHeight = divElem.offsetHeight - divElem.clientHeight;
	
	//dont keep this around
	divElem.remove();
	
}

function extendPath(){
  SVG.Element.prototype.draw.extend('path', {
    init:function(e){

        this.set = new SVG.Set();

        var p = this.startPoint,
        arr = [
          //"Move To" start point
                ["M", p.x, p.y]
              ];

        this.el.plot(arr);

    },

    // The calc-function sets the position of the last point to the mouse-position (with offset ofc)
    calc:function (e) {
        var arr = this.el.array().valueOf();

        if (e) {
            var p = this.transformPoint(e.clientX, e.clientY);
            //"Line To" next point
            arr.push(["L"].concat(this.snapToGrid([p.x, p.y])));
        }

        this.el.plot(arr);
    },

    clean:function(){

        // Remove all circles
        this.set.each(function () {
            this.remove();
        });

        this.set.clear();

        delete this.set;

    },

    });

}

function addExistingLayers(){
  let elementTypes = [
    "path",
    "g",
    "line",
    "circle",
    "image"
  ];

  var rootSvg = document.getElementById("root-svg");
	for(child of rootSvg.children){
    if(elementTypes.includes(child.nodeName) && child.getAttribute("data-ignore-layer")!="true"){
      addLayer(SVG.get(child.id));
    }
  }
}

SVG.on(document, 'DOMContentLoaded', function() {

  calcScrollbarsizes();

  extendPath();
    
  	updateSvgView();

	var domInput = document.getElementById("svgDOM")
	if( domInput != null && domInput.value !== "" ) {
		var svgParentNode = document.getElementById("svg-container-div");
		
		//escape and unescape are deprecated
		svgParentNode.innerHTML = unescape(domInput.value);
		var root = document.getElementById("root-svg");
		
		//SVG will readd these namespace attributes, it doesnt check if they
		//already exist, which leads to an error when it is sent back to the
		//server (duplicate attribute)
		root.removeAttribute("xmlns");
		root.removeAttribute("xmlns:svgjs");
   	 	root.removeAttribute("xmlns:xlink");
    
    	//store the updated markup
    	updateSvgView();
	}

	//this helps determine how the elements in the svg are stored, e.g. group contains marker and group with text and background
	//which could change over time and need to be handled in a different way in e.g. addLayer()
	document.getElementById("root-svg").setAttribute("data-svg-element-structure-version", "1");

    drawing = SVG("root-svg").size("100%", "100%");

    toolMap["draw"]["select-tool"] = document.getElementById("select-tool");
    toolMap["draw"]["text-tool"] = document.getElementById("text-tool");
    toolMap["draw"]["path-tool"] = document.getElementById("path-tool");
	  toolMap["draw"]["line-tool"] = document.getElementById("line-tool");
    toolMap["draw"]["circle-tool"] = document.getElementById("circle-tool");
	  toolMap["draw"]["layer-info-vis-toggle"] = document.getElementById("layer-info-vis-toggle");
    /*toolMap["draw"]["poly-tool"] = document.getElementById("poly-tool");*/

    toolMap["layer"]["edit-tool"] = document.getElementById("edit-tool");
    toolMap["layer"]["move-tool"] = document.getElementById("move-tool");
    toolMap["layer"]["send-to-front-tool"] = document.getElementById("send-to-front-tool");
    toolMap["layer"]["move-forward-tool"] = document.getElementById("move-forward-tool");
    toolMap["layer"]["move-backward-tool"] = document.getElementById("move-backward-tool");
    toolMap["layer"]["send-to-back-tool"] = document.getElementById("send-to-back-tool");
    toolMap["layer"]["void-tool"] = document.getElementById("void-tool");
    toolMap["layer"]["delete-tool"] = document.getElementById("delete-tool");
  
    var rootSvg = document.getElementById("root-svg");

    //supports selecting the default tool, generally path for signatures
    switch(rootSvg.getAttribute("data-default-tool")){
      
      case "select-tool":
        setSelectTool();
        break;

      case "text-tool":
        setTextTool();
        break;

      default:
      case "path-tool":
          setPathTool();
          break;
    }

    drawing.on('mousedown', function(e){
        //console.log("mousedown occurred, tool is ", tool);

        tool.draw(e);

    }, false);

    drawing.on('mouseup', function(e){
        //console.log("mouseup occurred, tool is ", tool);

        tool.draw('stop', e);
        
        updateSvgView();
    }, false);    

	addExistingLayers();
});

function updateSvgView(){
  var rootSvg = document.getElementById("root-svg");
  var newBounds = rootSvg.getBBox();

  var clientWidth = rootSvg.parentElement.clientWidth;
  var clientHeight = rootSvg.parentElement.clientHeight;

  var newWidth = Math.max(clientWidth, newBounds.width)-scrollbarWidth;
  var newHeight = Math.max(clientHeight, newBounds.height)-scrollbarHeight;

  //only expand the svg client area
  if(newWidth > pxToInt(rootSvg.style.width)) {
  	rootSvg.style.width = Number(newWidth+scrollbarWidth).toString()+"px";
  }
  
  if(newHeight > pxToInt(rootSvg.style.height)) {
  	rootSvg.style.height = Number(newHeight+scrollbarHeight).toString()+"px";
  }
  
  SVG("root-svg").size(rootSvg.style.width, rootSvg.style.height);
  
  //store the current svg in the input
  storeSVG();
  
}

function attachTool(){
    
    tool.on('drawstop', function(e){
      //console.log("drawstop occurrred");
      
      switch(tool.type){
          /*
            case "text":
              tool = drawing.text().attr('stroke',"black").attr('stroke-width',3).attr('fill','black');
              break;
          */
            case "line":
              //the tool instance becomes the new layer, after drawstop event a new instance will be referenced
              //by the "tool" variable
              addLayer(tool);
              tool = drawing.line().attr('stroke',"black").attr('stroke-width',3).attr('fill','none');
              break;
            
			case "circle":
              addLayer(tool);
              tool = drawing.circle().attr('stroke',"black").attr('stroke-width',3).attr('fill','none');
              break;
          	
			case "path":
              addLayer(tool);
              tool = drawing.path().attr('stroke',"black").attr('stroke-width',3).attr('fill','none');
              break;
			/*
			  case "polyline":
              addLayer(tool);
              tool = drawing.polyline().attr('stroke',"black").attr('stroke-width',3).attr('fill','none');
              break;
			*/
			
        }
        
        attachTool();
    }, tool);
}

function setToggle(buttonId, exclusiveGroup) {
  var pressedState = true;
  
  var button = document.getElementById(buttonId);

  //TODO evaluate layer UX
  //if this is not a type of layer operation
  if(exclusiveGroup !== "layer") {  	
    //deselect any selected elements
    selectElement(document.getElementById("root-svg"));
  }
  
    //remove highlighting if it exists, if setting select tool, it will be re-added
    var highlightStyle = document.getElementById("highlight-style");
    
    if(highlightStyle!==null) {
      highlightStyle.remove();
    }
  

  //if this button is a normal toggle, just toggle it
  if(exclusiveGroup===undefined && button.hasAttribute("aria-pressed")){
      pressedState = !button.getAttribute("aria-pressed");
  }

  button.setAttribute("aria-pressed", pressedState);

  setTextSelectableState(true);

  //if it's part of an exclusion group, set all the other toggles in the group to false
  if(exclusiveGroup !== undefined) {
    var otherButtons = Object.keys(toolMap[exclusiveGroup]).filter((id)=>id!=buttonId);
    
    otherButtons.forEach((id)=> {
        var pressedState = document.createAttribute("aria-pressed");
        pressedState.value=false;
        toolMap[exclusiveGroup][id].setAttributeNode(pressedState);
    });
  }
}

function addLayer(svgElement){

  updateSvgView();

  var layerList = document.getElementById("layer-list");
  var newRow = layerList.insertRow(1);

  newRow.setAttribute("id", svgElement.id() +"-layer-info");

  var cellVals = [
                {
                  type: "html",
                  value: "<input type='checkbox'/>",
                  func: function(elem) { 
                      var svgTarget = document.getElementById(svgElement.node.id);
                      var rootSvg = document.getElementById("root-svg");
                      elem.addEventListener("click", (event)=>{
                        //console.log(event);
                        if (event.currentTarget.firstElementChild.checked) {
                          svgTarget = getSelectableElement(svgTarget, true);
                          selectElement(svgTarget, true);
                        } else {
                          //this needs another arg to "deselect" to support multi-select correctly
                          selectElement(rootSvg, true);
                        }
                      });
                    
                    }
                },
                { 
                  type: "html", 
                  value:"<button type='button'><i class='material-icons'>visibility</i></button>",
                  func: function(cell) { cell.addEventListener("click", (elem)=>toggleVisibility(elem, svgElement.node)) }
                },
                { 
                  type: "txt",
                  value: svgElement.type.charAt(0) !== "g" ? svgElement.type.charAt(0).toUpperCase() + svgElement.type.substr(1) : "Text",
                  func: function (){}
                },
                {
                  type: "html",
                  value: "<svg width='100%' height='100%' viewbox='0 0 900 500'><use href=#"+svgElement.id()+"></use></svg>",
                  func: function(){}
                }
              ]

  for( var i = 0; i < cellVals.length; i++ ) {

      var cell = newRow.insertCell(i);
      var curCellVal = cellVals[i];
      var newChild;

      switch(curCellVal.type){
        case "txt":
          newChild = document.createTextNode(curCellVal.value);

          break;
        case "html":
          newChild = document.createRange().createContextualFragment(curCellVal.value);
          break;
      }

      curCellVal.func(cell);

      cell.appendChild(newChild);
  }
}

function setEnabledStates(buttonList, enabledStates){
  
  var idMap = {};
  buttonList.forEach((elem, i)=>{
                            idMap[elem.id]=i;
                        });

  var buttonIds = Array.from(buttonList).map(( elem )=>elem.id);

  var enabledKeys = Object.keys(enabledStates);

  if( enabledKeys.some((key)=>buttonIds.includes(key)) ) {
    enabledStatesArr = Array(buttonList.length).fill(true);
    enabledKeys.forEach((enabledKey)=>enabledStatesArr[idMap[enabledKey]]=enabledStates[enabledKey])
    
  } else if( ! (enabledStates instanceof Array) ) {
      enabledStatesArr = Array(buttonList.length).fill(enabledStates);
  } 

  buttonList.forEach((elem, i) => {
      if(!enabledStatesArr[i]){
      	elem.classList.add("disabled");
      } else {
      	elem.classList.remove("disabled");
      }
  });

}

var selectedElements = [
  /*
      {
        "elem": the selected element,
        "selectRect": the svg rect with bbox coords and marching ants, could alternately use drop shadow et c.
      }
  */
];

function selectElement(elem, allowDefault){
    console.log(elem);
    //adjust the type of selection behavior here, this is the default of remove select on all selected elements and select new element

    selectedElements.forEach((selectedInfo)=> {
        var selectedCheckbox = document.querySelector("#"+selectedInfo.elem.id+"-layer-info input[type='checkbox']");
        selectedCheckbox.checked = false;
        selectedInfo.selectRect.remove();
    });

    //deselect elements
    selectedElements = [];

    if(elem.id == "root-svg") {
      return false;
	}

	//if this is text, it's associated group will be in the layer list
	//this will still create the selection based on the text, but 
	//the group is required for the layer info
	
	var layerElem = elem;
	
	if(elem.nodeName == "text") {
		layerElem = elem.parentNode.parentNode;	
	}

	//check if this element has a visual layer
    var layerInfo = document.querySelector("#" + layerElem.id + "-layer-info");

    //if it doesn't, we won't select it
    if(layerInfo === null) {
        return false;
    }
	  
    var bbox = elem.getBBox();
    var padding = 10;
    //should replace hardcoded "padding" (+10, -5) with em conversions
    var selectRect = drawing.rect(bbox.width+padding, bbox.height+padding);

    if(layerElem.nodeName === "g") {
      var childGroupCollection = layerElem.getElementsByTagName("g");
      if(childGroupCollection.length > 0 ){
        var childGroup = childGroupCollection[0];
        childGroup.setAttribute("visibility", "visible");
      }
    }

    selectRect
      .attr("x", bbox.x-padding/2)
      .attr("y", bbox.y-padding/2)
      .attr("class", "selectedElement")
      .attr("stroke", "black")
      //for cursors (e.g. move) to work inside open figures, there must be a fill, but it can be completely transparent
      .attr("fill", "grey")
      .attr("fill-opacity", "0");
    
    selectedElements.push({"elem": layerElem , "selectRect":selectRect});

    if(selectedElements.length > 1){
        setEnabledStates(document.querySelectorAll(".layer-button"), {"edit-tool": false, "send-to-front-tool": false, "send-to-back-tool":false});
    }

    if(allowDefault!==true)
      event.preventDefault();

    //void-tool value should be determined based on the presence of data-obsid
    setEnabledStates(document.querySelectorAll(".layer-button"), {"void-tool":false});

    var selectedCheckbox = document.querySelector("#" + layerElem.id + "-layer-info input[type='checkbox']");
    selectedCheckbox.checked = true;
	
	return true;
}

function setTextSelectableState(selectable){
  var svgTextElements = document.querySelectorAll("svg text");

  if(selectable){
    svgTextElements.forEach((elem)=>elem.classList.remove("svgTextUnselectable"));
  }else {
    svgTextElements.forEach((elem)=>elem.classList.add("svgTextUnselectable"));
  }
}

function getSelectableElement(elem, returnSVG){
  elem = SVG.get(elem.id);

  //check if SVG was able to "get" an SVG.Element for the DOM element/node passed in
  if(elem === null){
    console.log("SVG.js was not able to get an SVG.Element for the specified node")
    return;
  }


  //select the whole group, since move will move all of it
  if(elem.type === "g"){
    
    //if there's no layer info, select the parent group instead
    if(document.querySelector("#"+ elem.id() +"-layer-info") == undefined){
      elem = elem.node.parentNode;
    }
    
    //elem = elem.node.getElementsByTagName("text")[0];
  } else if(elem.type === "use") {
    //select the entire parent <g> node
    elem = elem.node.parentNode;
    //.getElementsByTagName("text")[0];
  } else if(elem.type === "rect") {
                      //selecting an already selected element
    elem = selectedElements.filter((elemEntry)=>elemEntry.selectRect===elem)[0].elem;

  } else {
      //move up a tree if this is a child text span to get to the enclosing text element
      while (elem.type === "tspan") elem=elem.parent();

      elem = document.querySelector("#"+elem.id());
  }

  if(returnSVG && !(elem instanceof SVG.Element)) {
      elem = SVG.get(elem.id);
  }

  return elem;
}

function setSelectTool(event){
  setToggle("select-tool", "draw");

  //https://www.w3.org/wiki/Dynamic_style_-_manipulating_CSS_with_JavaScript

  //a style to highlight which svg element should be selected when clicking
  //this is applied when entering and should be removed when exiting select mode
  //as it can be distracting if it were always on
  var highlightOverRule = "svg *:hover { filter: url(#dropshadow); }";
  
  var highlightStyle = document.createElement("style");
  highlightStyle.setAttribute("id", "highlight-style");
  highlightStyle.innerHTML = highlightOverRule;
  
  document.body.appendChild(highlightStyle);

  tool = {
            draw: function(eOrMsg, e){
                //console.log(eOrMsg, e);
                if( eOrMsg instanceof Event){
                    //console.log(eOrMsg, eOrMsg.clientX, eOrMsg.clientY);
                    
                    var elem = document.elementFromPoint(eOrMsg.clientX, eOrMsg.clientY);
                    
                    elem = getSelectableElement(elem);

                    selectElement(elem);

                    //after an element is selected make text unselectable so it doesnt interact with dragging behavior
                    setTextSelectableState(false);
                } else if( typeof(eOrMsg) === "string" && eOrMsg === "stop") {
                    setTextSelectableState(true);
                } else {
                    console.log("msg", e);
                }
            },
            on: function(eventName, delegate, bind){}

  };
  attachTool();
}

function showTextPopup(left, top, mode, text) {
    var textareaPopup = document.getElementById("textarea-popup");
    textareaPopup.setAttribute("data-mode", mode);
    var x = left;
    var y = top;
    textareaPopup.style.display="block";
    textareaPopup.style.left = x + "px";
    textareaPopup.style.top = y + "px";

    var textarea = document.getElementById("annotation-text")
    
    if(text!==undefined) {
        textarea.value = text;
    }
    
    textarea.focus();
    event.preventDefault();
}

function setTextTool(){
  setToggle("text-tool", "draw");
  tool =  {
              draw: function(e){
                  if(e instanceof Event){

                      showTextPopup(e.clientX, e.clientY, "create");
                  }
              },
              on: function(eventName, delegate, bind){}
          };

  attachTool();
}

function toggleVisibility(event, svgElement) {
  
  if(svgElement === undefined) {
    return;
  }

  var elem = document.querySelector("#"+svgElement.id +"-layer-info i");
  if(elem.textContent === "visibility") {
    elem.textContent = "visibility_off";
    svgElement.style.display = "none";
  } else {
    elem.textContent = "visibility";
    svgElement.style.display = "inherit"
  }
  //this.getFirstChild().remove();

  
}

function pxToInt(str) {
  var res = 0;

  if(str.endsWith("px"))
    res = Number(str.substr(0, str.length-2));
  
    return res;
}

function createTextGroup(text, fontSize, x, y){
  var baseGroup = drawing.group();

  //baseGroup.translate(x, y);
  baseGroup
  	.use("pin-icon")
    .attr("x", x)
    .attr("y", y)
    .scale(0.25, 0.25, x, y);

  //create a second child group to make it easier to change the visibility
  var group = baseGroup.group();

  var padding = 5;

  //add the specific text
  var text = group
    .text(text)
    .attr("stroke","black")
    .attr("stroke-width",.5)
    .attr("fill","black")
    .font("size", fontSize)
    .attr("visibility", "inherit");

  var bounds = text.node.getBBox();
  
  var childGroupElementsY = y-bounds.height-padding;

  //after it's been inserted, and the bounds calculated, adjust position so it doesnt overlap the marker
  text
    .attr("x", x)
    .attr("y", childGroupElementsY);

  //add background rect so text is readable over whatever is behind it, subs another padding factor so text is "centered"
  group
    .rect(bounds.width+padding*2, bounds.height+padding*2)
    .attr("x", x-padding)
    .attr("y", childGroupElementsY-padding/2)
    .attr("fill", "white")
    .attr("stroke", "black")
    .radius(5)
    .attr("visibility", "inherit");

    //move the text infront of the background, it had to be created first for the bbox, but must be rendered after
    text.forward();

    group.attr("visibility", "collapse");

  return baseGroup;
}

function createText(){
  
  var textareaPopup = document.getElementById("textarea-popup");
  var mode = textareaPopup.getAttribute("data-mode");
  var textarea = document.querySelector("#textarea-popup #annotation-text");
  var text = textarea.value;
  var rootSvg = document.getElementById("root-svg");
  var rootSvgBBox = rootSvg.getBoundingClientRect();
  var parentOffsetX = rootSvgBBox.left;
  var parentOffsetY = rootSvgBBox.top;
  //console.log(parentOffsetX, parentOffsetY, rootSvg);

  var y = pxToInt(textareaPopup.style.top) - parentOffsetY;
  var x = pxToInt(textareaPopup.style.left) - parentOffsetX;

  var fontSize = window.getComputedStyle(textarea).fontSize;
  if(fontSize === undefined) {
  	fontSize = "1em";
  }

  var textGroup = createTextGroup(text, fontSize, x, y);
  
  addLayer(textGroup);

  //remove and select new area after adding layer so selection in layer list is updated
  if(mode==="edit") {
      var origId = textareaPopup.getAttribute("data-id");

      removeElement(SVG.get(origId));

      //var elemToSelect = getSelectableElement(textGroup.node);

      //selectElement(elemToSelect);
      selectElement(textGroup.node);
  }

  resetTextarea();
}

function resetTextarea(){
  document.querySelector("#textarea-popup #annotation-text").value = "";
  var textareaPopup = document.getElementById("textarea-popup");
  textareaPopup.style.display = "none";

}

function setPathTool(){
  setToggle("path-tool", "draw");
  
  tool = drawing.path().attr('stroke',"black").attr('stroke-width',3).attr('fill','none');
  attachTool();
}

function setLineTool(){
  setToggle("line-tool", "draw");
  
  tool = drawing.line().attr('stroke',"black").attr('stroke-width',3).attr('fill','none');
  attachTool();
}

function setCircleTool(){
  setToggle("circle-tool", "draw");

  tool = drawing.circle().attr('stroke',"black").attr('stroke-width',3).attr('fill','none');
  attachTool();
}

function clearAll(){
  var rootSvg = document.getElementById("root-svg");

  var unremovableCount = 0;

  //remove everything that can be selected except the current tool instance
  while(rootSvg.childElementCount > unremovableCount + 1) {
    var nonPermChild = rootSvg.children[unremovableCount];
    var svgElem = SVG.get(nonPermChild.id);
    if( selectElement(nonPermChild) ){
      removeElement(svgElem);
    } else {
      unremovableCount++;
    }
  }

}

/*function setPolyTool(){
  setToggle("poly-tool", "draw");

  tool = drawing.polyline().attr('stroke',"black").attr('stroke-width',3).attr('fill','none');
  attachTool();
}*/

function storeSVG(){
	
	//deep clone the svg node in case a selection rect needs to be removed from it
  var domClone = document.implementation.createHTMLDocument("");
  var node = domClone.importNode(document.getElementById("svg-container-div"), true);
  
  domClone.body.appendChild(node);

	if( selectedElements.length > 0 ) {
		selectedElements
			.forEach( function(selectionInfo) {
				  var clonedSelectRect = domClone.getElementById(selectionInfo.selectRect.id());
				  clonedSelectRect.remove();
			} )
	}
	
	var svgMarkup = domClone.getElementById("svg-container-div").innerHTML;
	var svgDOMInput = document.getElementById("svgDOM");

    svgDOMInput.value = svgMarkup;
}

function submitSave() {
	
	storeSVG();

    //need to add unique ids back for htmlformentry usage
    $j('#saveNotification').fadeIn(500).delay(2000).fadeOut(500);
    $j('#saveImageForm').submit();

}

function gatherText(elem) {

  var text = "";

  var len = elem.children;

  Array.from(elem.children).forEach((child, i)=>{
      text += child.textContent 
      
      if(i+1<len){
          text+= "\n";
      }
  });

  return text;
}

function setEditTool(){
  
  //edit should be disabled if multi-select for move/delete is ever implemented
  if(selectedElements.length > 1)
    return;

  setToggle("edit-tool", "layer");

  tool = {
            draw: function(eOrMsg, e) {

                var firstElem = selectedElements[0];
                
                //var elem = SVG.get(firstElem.elem.id);
                                      
                //move up a tree if this is a child text span to get to the enclosing text element
                //while (elem.type === "tspan") elem=elem.parent();

                //leave as an SVG object
                var svgElem = getSelectableElement(firstElem.elem, true);

                switch(svgElem.type) {
                    case "g":
                        var svgRoot = document.getElementById("root-svg");
                        var svgRootBBox = svgRoot.getBoundingClientRect();
                        var text = gatherText(svgElem.node.getElementsByTagName("text")[0]);
                        //console.log(svgRootBBox.x, svgRootBBox.y);
                        var textareaPopup = document.querySelector("#textarea-popup");
                        //store the top parent <g> group
                        var parentGroup = svgElem.node;

                        textareaPopup.setAttribute("data-id", parentGroup.id);
                        //get marker x,y since it is the original click positon
                        var marker = parentGroup.getElementsByTagName("use")[0];

                        showTextPopup(Number(marker.getAttribute("x"))+svgRootBBox.x, Number(marker.getAttribute("y"))+svgRootBBox.y, "edit", text);
                        break;

                }

            },
            on: function(eventName, delegate, bind){}
  }
}

function clipMove(delta, pointArray){

  var smallest = Math.min(pointArray);
  var newDelta = delta;

  if(delta+smallest < 0){
      newDelta = -smallest;
  }

  return newDelta;

}

function setMoveTool(){
  setToggle("move-tool", "layer");
  selectedElements.forEach((selectEntry)=>{
      selectEntry.selectRect.addClass("move");
  });

  tool =  {
              drawStart: undefined,
              draw: function(eOrMsg, e){
                if(eOrMsg instanceof Event){
                  //attach to mouse move?
                  this.drawStart = {"x": eOrMsg.clientX, "y": eOrMsg.clientY}

                }else if(typeof(eOrMsg)=="string" && eOrMsg==="stop") {

                    //these delta values will be clipped/saturated to 0,0,
                    //this isnt the only way to handle moving elements outside
                    //of the viewport, but it a simpler way
                    //than repositioning everything or trying to mess with 
                    //viewbox at this point
	
                    var deltaX = e.clientX - this.drawStart.x;
                    var deltaY = e.clientY - this.drawStart.y;

                    selectedElements.forEach((elemEntry)=>{
                        
                        var elem = getSelectableElement(elemEntry.elem, true);

                        //var elem = SVG.get(elemEntry.elem.id);
                        
                        //move up a tree if this is a child text span to get to the enclosing text element
                        //while (elem.type === "tspan") elem=elem.parent();

                        //instead, just get the bounding box
                        var pathBB = elem.node.getBBox();

                        //use it's upper-left x and y
                        var prevX = pathBB.x;
                        var prevY = pathBB.y;

                        //clip the movement to [(0,0), (\inf, \inf))
                        deltaX = clipMove(deltaX, [prevX]);
                        deltaY = clipMove(deltaY, [prevY]);

                        //use svg.js' dmove()
                        elem.dmove(deltaX, deltaY);

                        elem = elemEntry.selectRect;

                        prevX = elem.attr("x");
                        prevY = elem.attr("y");

                        //allow the select rect to go outside the visible area, since it is only a temporary internal object

                        elem.attr("x", prevX+deltaX);
                        elem.attr("y", prevY+deltaY);

                    });

                    this.drawStart = undefined;
                }
              },
              on: function(eventName, delegate, bind){}
          };

  attachTool();

}

function removeSvgAndLayer(svgElem, layerRowId){
  svg.remove()
}

function removeElement(svgElem) {
  var layerList = document.getElementById("layer-list");
  
  var rootSvg = document.getElementById("root-svg");
  
  //build a map from id to index 
  var idIndexMap = {};
  var indexIdMap = {};

  Array.from(layerList.rows)
      .forEach((row,i)=>{
        idIndexMap[row.id]=i;
        indexIdMap[i]=row.id;
        });

  //selecting root deselects all elements
  selectElement(rootSvg);
  
  if(!(svgElem instanceof Array)) {
      svgElem = [svgElem];
  }

  svgElem.forEach((svg)=>{ 

      var layerInfoId = svg.id()+"-layer-info";

      //must be patched after every deleteRow (indices > curIndex must be decremented)
      //is it better to patch these every time or cmp id strings every time on larger layer lists?
      var index = idIndexMap[layerInfoId];
      //just overwrites, last layer set undefined for gc? delete or leave dupe?
      //delete indexIdMap[index];
      
      var curKeys = Object.keys(indexIdMap);
      curKeys.forEach(function(curIndex){
          if(curIndex>index) {
            
            if(curIndex > curKeys.length) {
                indexIdMap[curIndex] = undefined;
            }

            var curId =indexIdMap[curIndex];

            indexIdMap[curIndex-1] = curId;
            idIndexMap[curId] = curIndex-1;
            
          }
      });

      layerList.deleteRow(index);

      svg.remove();
  });

  setEnabledStates(document.querySelectorAll(".layer-button"), false);

  updateSvgView();

}

function setDeleteTool() {
    var svgElems = selectedElements
      .map((elemEntry)=>SVG.get(elemEntry.elem.id));
    removeElement(svgElems);
}

function toggleLayerInfoVis(){
  setToggle("layer-info-vis-toggle");

  var layerPanel = document.querySelector("#arrange");
  if(layerPanel.classList.contains("reveal")) {
      layerPanel.classList.remove("reveal");
  } else {
      layerPanel.classList.add("reveal");
  }
}

function selectTemplate(){
    var templatePopup = document.querySelector("#template-image-popup");
    templatePopup.style.display = "visible";
    //templatePopup
}

function layerSendToFront() {
  selectedElements.forEach((selectEntry)=>{
    SVG.get(selectEntry.elem.id).front();
    selectEntry.selectRect.front();
  });
}

function layerForward() {
  selectedElements.forEach((selectEntry)=>{
    SVG.get(selectEntry.elem.id).forward();
    selectEntry.selectRect.forward();
  });
}

function layerBackward() {
  selectedElements.forEach((selectEntry)=>{
    SVG.get(selectEntry.elem.id).backward();
    selectEntry.selectRect.backward();
  });
}

function layerSendToBack() {
  selectedElements.forEach((selectEntry)=>{
    SVG.get(selectEntry.elem.id).back();
    selectEntry.selectRect.back();
  });
}

document.querySelector("#load-image-file-input").addEventListener('change', function(e) {
            if (window.File && window.FileReader && window.FileList && window.Blob) {
                var reader = new FileReader();
                reader.onload = function(event) {
                    var img = new Image();
                    
                    var imageElement = drawing.image(event.target.result);

                    addLayer(imageElement);
                }
                reader.readAsDataURL(e.target.files[0]);
            } else {
                alert('your browser does not support on-the-fly file upload');
            }
        });