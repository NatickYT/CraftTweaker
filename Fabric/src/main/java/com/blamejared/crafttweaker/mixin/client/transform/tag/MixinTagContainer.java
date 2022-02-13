package com.blamejared.crafttweaker.mixin.client.transform.tag;

import com.blamejared.crafttweaker.api.ScriptLoadingOptions;
import com.blamejared.crafttweaker.api.tag.registry.CrTTagRegistry;
import net.minecraft.tags.TagContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TagContainer.class)
public class MixinTagContainer {
    
    @Inject(method = "bindToGlobal", at = @At("RETURN"))
    public void updateTags(CallbackInfo ci) {
    
        CrTTagRegistry.INSTANCE.setCurrentTagContainer(() -> (TagContainer) (Object)this);
        ScriptLoadingOptions.ClientScriptLoader.updateTags();
    }
    
}
