package com.blamejared.crafttweaker_annotation_processors.processors.document.conversion.converter.expansion.member;

import com.blamejared.crafttweaker_annotation_processors.processors.document.conversion.converter.member.MemberConverter;
import com.blamejared.crafttweaker_annotation_processors.processors.document.page.info.DocumentationPageInfo;
import com.blamejared.crafttweaker_annotation_processors.processors.document.page.member.virtual_member.DocumentedVirtualMembers;
import com.blamejared.crafttweaker_annotation_processors.processors.util.dependencies.DependencyContainer;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.util.Elements;

public class ExpansionVirtualMemberConverter extends MemberConverter<DocumentedVirtualMembers> {
    
    public ExpansionVirtualMemberConverter(DependencyContainer dependencyContainer, Elements elements) {
        
        super(elements);
        //TODO: Add converters
        addElementConverter(ElementKind.METHOD, dependencyContainer.getInstanceOfClass(ExpansionMethodConverter.class));
        addElementConverter(ElementKind.METHOD, dependencyContainer.getInstanceOfClass(ExpansionCasterConverter.class));
        addElementConverter(ElementKind.METHOD, dependencyContainer.getInstanceOfClass(ExpansionGetterSetterConverter.class));
    }
    
    @Override
    protected DocumentedVirtualMembers createResultObject(DocumentationPageInfo pageInfo) {
        
        return new DocumentedVirtualMembers();
    }
    
    @Override
    protected boolean isCandidate(Element enclosedElement) {
        
        return enclosedElement.getModifiers().contains(Modifier.STATIC);
    }
    
}
