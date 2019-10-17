<%@ include file="/WEB-INF/template/include.jsp"%>

<openmrs:require privilege="Edit Observations" otherwise="/login.htm"  />
<openmrs:htmlInclude file="/moduleResources/drawing/colorpicker.css"/>
<openmrs:htmlInclude file="/moduleResources/drawing/paint.css"/>

<openmrs:htmlInclude file="/moduleResources/drawing/colorpicker.js"/>
<openmrs:htmlInclude file="/moduleResources/drawing/paint.js"/>
<openmrs:htmlInclude file="/moduleResources/drawing/resize.js"/>

<openmrs:htmlInclude file="/scripts/calendar/calendar.js" />
<openmrs:htmlInclude file="/scripts/timepicker/timepicker.js" />

<openmrs:htmlInclude file="/moduleResources/drawing/svg.js"/>
<openmrs:htmlInclude file="/moduleResources/drawing/svg.draw.js"/>

<openmrs:htmlInclude file="/moduleResources/drawing/style.css"/>

<c:choose>
            <c:when test="${obsId == null}" >
        <div id="drawingObsform" >
         <form method="post" id="saveImageForm" action="<openmrs:contextPath/>/module/drawing/saveDrawing.form">
        
             <table>
                 <c:choose>
                     <c:when test="${model.patientId == null}">
                         <tr>
                          <td><spring:message code="drawing.patient"/></td>
                         <td><openmrs_tag:personField formFieldName="patientId" formFieldId="drawingPatientId" searchLabelCode="Person.findBy"  linkUrl="" callback="" /></td>
                        </tr>
                    </c:when>
                    <c:otherwise>
                      <input type="hidden" name="patientId" value='${model.patientId}' />
                    </c:otherwise>
                  </c:choose>
               <tr>
                      <td><spring:message code="drawing.questionConcept"/></td>
                <td>
                     <openmrs:globalProperty var="questionConcepts" key="drawing.questionConcepts" listSeparator=","/>
                 
                     <c:choose>
                         <c:when test="${empty questionConcepts}">
                             <openmrs_tag:conceptField formFieldName="conceptId" formFieldId="drawingConceptId" includeDatatypes="Complex" includeClasses="Drawing"/>
                        </c:when>
                         <c:otherwise>
                             <select id="drawingConceptId" name="conceptId">
                                 <c:forEach var="conceptId" items="${questionConcepts}">
                                   <option value="${conceptId}"><openmrs:format conceptId="${conceptId}"/></option>
                                 </c:forEach>
                             </select>
                         </c:otherwise>
                     </c:choose>
                 </td>
             </tr>
                           <tr>
                           <td><spring:message code="drawing.encounter"/></td>
                           <td><openmrs_tag:encounterField formFieldName="encounterId" formFieldId="drawingEncounterId" /> </td>
                           </tr>
             <tr>
                  <td><spring:message code="drawing.date"/></td>
                   <td><input type="text" name="date" size="10" onfocus="showCalendar(this)" id="drawingDate" />(<spring:message code="general.format"/>: <openmrs:datePattern />)</td>
            </tr>
            
        </table>
            <input type="hidden" id="svgDOM" name="svgDOM"/>
            <input type="hidden" name="redirectUrl" value="${model.redirectUrl}">
        </form>
        </div>
        </c:when>
        <c:otherwise>
        <form method="post" id="saveImageForm" action="<openmrs:contextPath/>/module/drawing/updateDrawing.form">
           <input type="hidden" id="svgDOM" name="svgDOM" value="${svgDOM}"/>
           <input type="hidden" id="obsId" name="obsId" value="${obsId} "/>
           </form>
        </c:otherwise>
        </c:choose>
    
         <div class="editorContainer">
         <div id="drawingHeader">
			 <div id="colorSelector"  style="float: left" class="colorselector tool" title="Color Picker">
                    <div class="colorselector_innerdiv"></div>     
             </div>
             <div style="clear:both;"></div>
              
        </div>
        <div id="svgDiv" class="svgDiv">
            <%@ include file="/WEB-INF/view/module/drawing/resources/SVG.html" %>
			<openmrs:htmlInclude file="/moduleResources/drawing/ui.js"/>
        </div>
		<div id="templatesDialog" title="Templates" style="display:none;position:relative">
		<c:choose>
		<c:when test="${not empty model.encodedTemplateNames}">
		<div style="position:relative">
				<div style="width:30%;height:100%;float:left;border:1px;;margin-bottom:10px">
				    <b class="boxHeader"><spring:message code="drawing.availableTemplates"/></b>
					<div class="box" style="height:350px">
					 Search:<input type="search" id="searchTemplates" placeholder="search..."/>
						<div style="overflow-y: scroll;overflow-x:hidden;height:315px">
						
		       				<table>
		       				 <c:forEach var="encodedTemplateName" items="${model.encodedTemplateNames}">
							 <tr>
							     <td style="display:list-item;list-style:disc inside;"></td>
                    			 <td class="templateName" style="cursor:pointer">${encodedTemplateName}</td>
                 			  </tr>
							 </c:forEach>
               				</table>
						</div>
					</div>
				</div>
				<div style="float:left;width:68%;margin-left:10px;margin-bottom:10px" >
					<b class="boxHeader"><spring:message code="drawing.preview"/></b>
					<div class="box" style="height:350px">
		        		 <img  src="<openmrs:contextPath/>/moduleResources/drawing/images/preview.png" id="templateImage" class="templateImage"/>

					</div>
				</div>
				
			</div>
		<div style="clear:both"></div>
		</c:when>
		<c:otherwise>
		     <spring:message code="drawing.noTemplatesUploaded"/>
		</c:otherwise>
		</c:choose>
		
		</div>
        
        <div id="drawingFooter">
             <div class="tool">
              <spring:message code="drawing.clearCanvas"/>
			  <input type="button" id="showTemplates" value="<spring:message code="drawing.showTemplates"/>"/> 
              <span id='saveNotification' style='display:none;color:#ffffff;float:right'><spring:message code="drawing.saved"/></span>
            </div>        
        </div>
   </div>