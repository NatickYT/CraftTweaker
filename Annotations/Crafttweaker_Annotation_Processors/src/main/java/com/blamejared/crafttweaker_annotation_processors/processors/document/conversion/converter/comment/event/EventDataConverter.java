package com.blamejared.crafttweaker_annotation_processors.processors.document.conversion.converter.comment.event;

import com.blamejared.crafttweaker_annotation_processors.processors.document.conversion.converter.comment.DeprecationFinder;
import com.blamejared.crafttweaker_annotation_processors.processors.document.conversion.converter.comment.SinceInformationIdentifier;
import com.blamejared.crafttweaker_annotation_processors.processors.document.conversion.converter.comment.documentation_parameter.ParameterInfo;
import com.blamejared.crafttweaker_annotation_processors.processors.document.conversion.converter.comment.documentation_parameter.ParameterInformationList;
import com.blamejared.crafttweaker_annotation_processors.processors.document.conversion.converter.comment.documentation_parameter.ParameterReader;
import com.blamejared.crafttweaker_annotation_processors.processors.document.meta.MetaData;
import com.blamejared.crafttweaker_annotation_processors.processors.document.page.comment.DocumentationComment;
import com.blamejared.crafttweaker_annotation_processors.processors.document.page.member.header.examples.ExampleData;

import javax.lang.model.element.Element;

public class EventDataConverter {
    
    private static final String DOC_EVENT = "docEvent";
    private static final String CANCELED = "canceled";
    private static final String NOT_CANCELED = "notCanceled";
    private static final String ALLOW = "allow";
    private static final String DEFAULT = "default";
    private static final String DENY = "deny";
    private static final String NOT_CANCELED_INFO = "The event is not cancelable.";
    private static final String NO_RESULT_INFO = "The event does not have a result.";
    private static final String CANCELED_INFO = "The event is cancelable.";
    private static final String HAS_RESULT_INFO = "The event has a result.";
    
    private final ParameterReader reader;
    private final DeprecationFinder deprecationFinder;
    private final SinceInformationIdentifier sinceInformationIdentifier;
    
    public EventDataConverter(final ParameterReader reader, final DeprecationFinder deprecationFinder,
                              final SinceInformationIdentifier sinceInformationIdentifier) {
        
        this.reader = reader;
        this.deprecationFinder = deprecationFinder;
        this.sinceInformationIdentifier = sinceInformationIdentifier;
    }
    
    public DocumentationComment getDocumentationComment(String docComment, Element element) {
        
        ParameterInformationList parameterInformationList = reader.readParametersFrom(docComment, element);
        if(!hasEventData(parameterInformationList)) {
            return new DocumentationComment(
                    NOT_CANCELED_INFO + "\n\n" + NO_RESULT_INFO,
                    null,
                    null,
                    ExampleData.empty(),
                    MetaData.empty()
            );
        }
        StringBuilder sb = new StringBuilder();
        ParameterInfo parameterInfo = getEventDataInfo(parameterInformationList);
        if(!hasCancelledInfo(parameterInfo)) {
            sb.append(NOT_CANCELED_INFO);
            sb.append("\n\n");
        } else {
            sb.append(CANCELED_INFO);
            sb.append("\n\n");
            for(String occurrence : parameterInfo.getOccurrences()) {
                switch(getTypeNameFrom(occurrence)) {
                    case CANCELED:
                        sb.append("If the event is canceled, ");
                        sb.append(getTypeTextValueFrom(occurrence));
                        break;
                    case NOT_CANCELED:
                        sb.append("If the event is not canceled, ");
                        sb.append(getTypeTextValueFrom(occurrence));
                        break;
                }
                sb.append("\n\n");
            }
        }
        if(!hasResultInfo(parameterInfo)) {
            sb.append(NO_RESULT_INFO);
            sb.append("\n\n");
        } else {
            sb.append(HAS_RESULT_INFO);
            sb.append("\n\n");
            for(String occurrence : parameterInfo.getOccurrences()) {
                switch(getTypeNameFrom(occurrence)) {
                    case ALLOW:
                        sb.append("If result is set to `allow`, ");
                        sb.append(getTypeTextValueFrom(occurrence));
                        break;
                    case DEFAULT:
                        sb.append("If result is set to `default`, ");
                        sb.append(getTypeTextValueFrom(occurrence));
                        break;
                    case DENY:
                        sb.append("If result is set to `deny`, ");
                        sb.append(getTypeTextValueFrom(occurrence));
                        break;
                }
                sb.append("\n\n");
            }
        }
        return new DocumentationComment(
                sb.toString(),
                this.deprecationFinder.findInCommentString(docComment, element),
                this.sinceInformationIdentifier.findInCommentString(docComment, element),
                ExampleData.empty(),
                MetaData.empty()
        );
    }
    
    private boolean hasEventData(ParameterInformationList parameterInformationList) {
        
        return parameterInformationList.hasParameterInfoWithName(DOC_EVENT);
    }
    
    private ParameterInfo getEventDataInfo(ParameterInformationList parameterInformationList) {
        
        return parameterInformationList.getParameterInfoWithName(DOC_EVENT);
    }
    
    private boolean hasCancelledInfo(ParameterInfo parameterInfo) {
        
        return parameterInfo.getOccurrences().stream().anyMatch(s -> getTypeNameFrom(s).equals(CANCELED));
    }
    
    private boolean hasResultInfo(ParameterInfo parameterInfo) {
        
        return parameterInfo.getOccurrences().stream().anyMatch(s -> {
            String type = getTypeNameFrom(s);
            return type.equals(ALLOW) || type.equals(DEFAULT) || type.equals(DENY);
        });
    }
    
    private String[] splitOccurrence(String occurrence) {
        
        return occurrence.split(" ", 2);
    }
    
    private String getTypeNameFrom(String occurrence) {
        
        return splitOccurrence(occurrence)[0];
    }
    
    private String getTypeTextValueFrom(String occurrence) {
        
        return splitOccurrence(occurrence)[1];
    }
    
}
