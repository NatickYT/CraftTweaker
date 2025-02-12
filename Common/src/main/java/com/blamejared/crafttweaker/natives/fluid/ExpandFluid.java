package com.blamejared.crafttweaker.natives.fluid;

import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker.api.tag.MCTag;
import com.blamejared.crafttweaker.platform.Services;
import com.blamejared.crafttweaker_annotations.annotations.Document;
import com.blamejared.crafttweaker_annotations.annotations.NativeTypeRegistration;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import org.openzen.zencode.java.ZenCodeType;

@ZenRegister
@Document("vanilla/api/fluid/Fluid")
@NativeTypeRegistration(value = Fluid.class, zenCodeName = "crafttweaker.api.fluid.Fluid")
public class ExpandFluid {
    
    @ZenCodeType.Method
    @ZenCodeType.Getter("bucket")
    public static Item getBucket(Fluid internal) {
        
        return internal.getBucket();
    }
    
    @ZenCodeType.Method
    @ZenCodeType.Operator(ZenCodeType.OperatorType.EQUALS)
    public static boolean isSame(Fluid internal, Fluid other) {
        
        return internal.isSame(other);
    }
    
    @ZenCodeType.Method
    public static boolean isIn(Fluid internal, MCTag<Fluid> tag) {
        
        return internal.is(tag.getInternal());
    }
    
    @ZenCodeType.Getter("commandString")
    public static String getCommandString(Fluid internal) {
        
        return "<fluid:" + Services.REGISTRY.getRegistryKey(internal) + ">.definition";
    }
    
}
