package com.blamejared.crafttweaker.api.zencode.impl.registry;

import com.blamejared.crafttweaker.api.CraftTweakerAPI;
import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker.api.natives.CrTNativeTypeInfo;
import com.blamejared.crafttweaker.api.natives.NativeTypeRegistry;
import com.blamejared.crafttweaker.api.recipe.manager.base.IRecipeManager;
import com.blamejared.crafttweaker.platform.Services;
import com.blamejared.crafttweaker_annotations.annotations.NativeMethod;
import com.blamejared.crafttweaker_annotations.annotations.NativeTypeRegistration;
import com.blamejared.crafttweaker_annotations.annotations.TypedExpansion;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.openzen.zencode.java.ZenCodeGlobals;
import org.openzen.zencode.java.ZenCodeType;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ZenClassRegistry {
    
    private final NativeTypeRegistry nativeTypeRegistry = new NativeTypeRegistry();
    
    /**
     * All classes with @ZenRegister and their modDeps fulfilled
     */
    private final List<Class<?>> allRegisteredClasses = new ArrayList<>();
    
    /**
     * All classes that caused errors when evaluating
     * Used to skip incompatible classes instead of throwing multiple Errors
     * (One error will always be thrown when validating at the beginning!)
     */
    private final Set<Class<?>> blacklistedClasses = new HashSet<>();
    
    /**
     * All classes that have at least one global field, with their @Name as the key
     */
    private final BiMap<String, Class<?>> zenGlobals = HashBiMap.create();
    
    /**
     * All Classes with @Name, key is @Name#value
     */
    private final BiMap<String, Class<?>> zenClasses = HashBiMap.create();
    
    /**
     * All classes with @Expansion, grouped by @Expansion#value
     */
    private final Map<String, List<Class<?>>> expansionsByExpandedName = new HashMap<>();
    
    
    public List<Class<? extends IRecipeManager>> getRecipeManagers() {
        
        return getImplementationsOf(IRecipeManager.class);
    }
    
    public boolean isRegistered(Class<?> cls) {
        
        return zenClasses.inverse().containsKey(cls);
    }
    
    public String getNameFor(Class<?> cls) {
        
        return zenClasses.inverse().get(cls);
    }
    
    public Optional<String> tryGetNameFor(Class<?> cls) {
        
        if(isRegistered(cls)) {
            return Optional.ofNullable(getNameFor(cls));
        }
        return Optional.empty();
    }
    
    public <T> List<Class<? extends T>> getImplementationsOf(Class<T> checkFor) {
        //Cast okay because of isAssignableFrom
        return allRegisteredClasses.stream()
                .filter(checkFor::isAssignableFrom)
                .filter(cls -> !cls.isInterface() && !Modifier.isAbstract(cls.getModifiers()))
                .map(cls -> (Class<? extends T>) cls)
                .collect(Collectors.toList());
    }
    
    public List<Class<?>> getAllRegisteredClasses() {
        
        return allRegisteredClasses;
    }
    
    public BiMap<String, Class<?>> getZenGlobals() {
        
        return zenGlobals;
    }
    
    public BiMap<String, Class<?>> getZenClasses() {
        
        return zenClasses;
    }
    
    public Map<String, List<Class<?>>> getExpansionsByExpandedName() {
        
        return expansionsByExpandedName;
    }
    
    public void addClass(Class<?> cls) {
        
        if(areModsMissing(cls.getAnnotation(ZenRegister.class))) {
            final String canonicalName = cls.getCanonicalName();
            CraftTweakerAPI.LOGGER.debug("Skipping class '{}' since its Mod dependencies are not fulfilled", canonicalName);
            return;
        }
        
        if(isIncompatible(cls)) {
            blacklistedClasses.add(cls);
            return;
        }
        
        allRegisteredClasses.add(cls);
        
        if(cls.isAnnotationPresent(ZenCodeType.Name.class)) {
            addZenClass(cls);
        }
        
        if(cls.isAnnotationPresent(ZenCodeType.Expansion.class)) {
            final String expandedClassName = cls.getAnnotation(ZenCodeType.Expansion.class).value();
            addExpansion(cls, expandedClassName);
        }
        
        if(cls.isAnnotationPresent(TypedExpansion.class)) {
            addTypedExpansion(cls);
        }
        
        if(hasGlobals(cls)) {
            if(cls.isAnnotationPresent(ZenCodeType.Name.class)) {
                addGlobal(cls);
            } else {
                CraftTweakerAPI.LOGGER.warn("Class: '{}' has a Global value, but is missing the '{}' annotation! Please report this to the mod author!", cls, ZenCodeType.Name.class.getName());
            }
        }
    }
    
    /**
     * Checks that the class does not have any fields or methods that would cause errors when converting in the JavaNativeModule.
     * Does so by simply calling the declaredMethods and fields getters.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private boolean isIncompatible(Class<?> cls) {
        
        try {
            cls.getDeclaredFields();
            cls.getFields();
            cls.getDeclaredMethods();
            cls.getMethods();
            cls.getConstructors();
            cls.getDeclaredConstructors();
            return false;
        } catch(Throwable t) {
            CraftTweakerAPI.LOGGER.error("Could not register class '{}'! This is most likely a compatibility issue!", cls
                    .getCanonicalName(), t);
            return true;
        }
    }
    
    private void addTypedExpansion(Class<?> cls) {
        
        final TypedExpansion annotation = cls.getAnnotation(TypedExpansion.class);
        final Class<?> expandedType = annotation.value();
        
        if(nativeTypeRegistry.hasInfoFor(expandedType)) {
            final String expandedClassName = nativeTypeRegistry.getCrTNameFor(expandedType);
            addExpansion(cls, expandedClassName);
        } else if(expandedType.isAnnotationPresent(ZenCodeType.Name.class)) {
            final ZenCodeType.Name nameAnnotation = expandedType.getAnnotation(ZenCodeType.Name.class);
            final String expandedClassName = nameAnnotation.value();
            addExpansion(cls, expandedClassName);
        } else {
            final String expandedTypeClassName = expandedType.getCanonicalName();
            CraftTweakerAPI.LOGGER.error("Cannot add Expansion for '{}' as the expanded type is not registered!", expandedTypeClassName);
        }
    }
    
    private void addNativeAnnotation(Class<?> cls) {
        
        final NativeTypeRegistration annotation = cls.getAnnotation(NativeTypeRegistration.class);
        final String zenCodeName = annotation.zenCodeName();
        nativeTypeRegistry.addNativeType(annotation, cls.getAnnotationsByType(NativeMethod.class));
        addExpansion(cls, zenCodeName);
    }
    
    private boolean areModsMissing(ZenRegister register) {
        
        return register == null || !Arrays.stream(register.modDeps())
                .filter(modId -> modId != null && !modId.isEmpty())
                .allMatch(Services.PLATFORM::isModLoaded);
    }
    
    private void addZenClass(Class<?> cls) {
        
        final ZenCodeType.Name annotation = cls.getAnnotation(ZenCodeType.Name.class);
        final String name = annotation.value();
        if(zenClasses.containsKey(name)) {
            final Class<?> otherCls = zenClasses.get(name);
            CraftTweakerAPI.LOGGER.error("Duplicate ZenCode Name '{}' in classes '{}' and '{}'", name, otherCls, cls);
        }
        
        zenClasses.put(name, cls);
        CraftTweakerAPI.LOGGER.debug("Registering '{}'", name);
    }
    
    private void addGlobal(Class<?> cls) {
        
        final ZenCodeType.Name annotation = cls.getAnnotation(ZenCodeType.Name.class);
        zenGlobals.put(annotation.value(), cls);
    }
    
    private void addExpansion(Class<?> cls, String expandedClassName) {
        
        if(!expansionsByExpandedName.containsKey(expandedClassName)) {
            expansionsByExpandedName.put(expandedClassName, new ArrayList<>());
        }
        expansionsByExpandedName.get(expandedClassName).add(cls);
    }
    
    private boolean hasGlobals(Class<?> cls) {
        
        return Stream.concat(Arrays.stream(cls.getFields()), Arrays.stream(cls.getMethods()))
                .filter(member -> member.isAnnotationPresent(ZenCodeGlobals.Global.class))
                .filter(member -> Modifier.isPublic(member.getModifiers()))
                .anyMatch(member -> Modifier.isStatic(member.getModifiers()));
    }
    
    public List<Class<?>> getClassesInPackage(String name) {
        
        return zenClasses.entrySet()
                .stream()
                .filter(entry -> entry.getKey().startsWith(name))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }
    
    public List<Class<?>> getGlobalsInPackage(String name) {
        
        return zenGlobals.entrySet()
                .stream()
                .filter(entry -> entry.getKey().startsWith(name))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }
    
    public Set<String> getRootPackages() {
        
        return zenClasses.keySet()
                .stream()
                .map(key -> key.split("\\.", 2)[0])
                .collect(Collectors.toSet());
    }
    
    public void addNativeType(Class<?> cls) {
        
        if(areModsMissing(cls.getAnnotation(ZenRegister.class))) {
            return;
        }
        
        if(cls.isAnnotationPresent(NativeTypeRegistration.class)) {
            addNativeAnnotation(cls);
        }
    }
    
    public void initNativeTypes() {
        
        for(CrTNativeTypeInfo nativeTypeInfo : nativeTypeRegistry.getNativeTypeInfos()) {
            final String craftTweakerName = nativeTypeInfo.getCraftTweakerName();
            final String vanillaClass = nativeTypeInfo.getVanillaClass().getCanonicalName();
            
            zenClasses.put(craftTweakerName, nativeTypeInfo.getVanillaClass());
            CraftTweakerAPI.LOGGER.debug("Registering {} for native type '{}'", craftTweakerName, vanillaClass);
        }
    }
    
    public NativeTypeRegistry getNativeTypeRegistry() {
        
        return nativeTypeRegistry;
    }
    
    public boolean isBlacklisted(Class<?> cls) {
        
        return blacklistedClasses.contains(cls);
    }
    
}
