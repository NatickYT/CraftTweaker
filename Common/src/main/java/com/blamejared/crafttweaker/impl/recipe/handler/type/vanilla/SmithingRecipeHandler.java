package com.blamejared.crafttweaker.impl.recipe.handler.type.vanilla;

import com.blamejared.crafttweaker.api.CraftTweakerRegistry;
import com.blamejared.crafttweaker.api.ingredient.IIngredient;
import com.blamejared.crafttweaker.api.recipe.handler.IRecipeHandler;
import com.blamejared.crafttweaker.api.recipe.handler.IReplacementRule;
import com.blamejared.crafttweaker.api.recipe.manager.base.IRecipeManager;
import com.blamejared.crafttweaker.api.util.IngredientUtil;
import com.blamejared.crafttweaker.api.util.ItemStackUtil;
import com.blamejared.crafttweaker.api.util.StringUtils;
import com.blamejared.crafttweaker.mixin.common.access.recipe.AccessUpgradeRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.UpgradeRecipe;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@IRecipeHandler.For(UpgradeRecipe.class)
public final class SmithingRecipeHandler implements IRecipeHandler<UpgradeRecipe> {
    
    @Override
    public String dumpToCommandString(final IRecipeManager manager, final UpgradeRecipe recipe) {
        
        return String.format(
                "smithing.addRecipe(%s, %s, %s, %s);",
                StringUtils.quoteAndEscape(recipe.getId()),
                ItemStackUtil.getCommandString(recipe.getResultItem()),
                IIngredient.fromIngredient(((AccessUpgradeRecipe)recipe).getBase()).getCommandString(),
                IIngredient.fromIngredient(((AccessUpgradeRecipe)recipe).getAddition()).getCommandString()
        );
    }
    
    @Override
    public Optional<Function<ResourceLocation, UpgradeRecipe>> replaceIngredients(final IRecipeManager manager, final UpgradeRecipe recipe, final List<IReplacementRule> rules) {
        
        final Optional<Ingredient> base = IRecipeHandler.attemptReplacing(((AccessUpgradeRecipe)recipe).getBase(), Ingredient.class, recipe, rules);
        final Optional<Ingredient> addition = IRecipeHandler.attemptReplacing(((AccessUpgradeRecipe)recipe).getAddition(), Ingredient.class, recipe, rules);
        
        if(!base.isPresent() && !addition.isPresent()) {
            return Optional.empty();
        }
        
        return Optional.of(id -> new UpgradeRecipe(id, base.orElseGet(() -> ((AccessUpgradeRecipe)recipe).getBase()), addition.orElseGet(() -> ((AccessUpgradeRecipe)recipe).getAddition()), recipe.getResultItem()));
    }
    
    @Override
    public <U extends Recipe<?>> boolean doesConflict(final IRecipeManager manager, final UpgradeRecipe firstRecipe, final U secondRecipe) {
        
        if(!(secondRecipe instanceof UpgradeRecipe)) {
            
            // If it is not an instanceof the normal recipe class, it means it has been added by a mod. To ensure symmetry,
            // we redirect to the other recipe handler. We are sure this cannot cause an infinite loop because recipe
            // handlers are checked based on the recipe class or superclasses. If secondRecipe were to redirect here,
            // it means that the if above would be false. If secondRecipe were not to have a recipe handler, it would
            // get the default of "never conflicts" anyway. Last possibility is for secondRecipe to have a mod-added
            // handler, and infinite loops are now up to them
            return this.redirectNonVanilla(manager, secondRecipe, firstRecipe);
        }
        
        final UpgradeRecipe second = (UpgradeRecipe) secondRecipe;
        
        return IngredientUtil.canConflict(((AccessUpgradeRecipe)firstRecipe).getBase(), ((AccessUpgradeRecipe)second).getBase()) && IngredientUtil.canConflict(((AccessUpgradeRecipe)firstRecipe).getAddition(), ((AccessUpgradeRecipe)second).getAddition());
    }
    
    private <T extends Recipe<?>> boolean redirectNonVanilla(final IRecipeManager manager, final T second, final UpgradeRecipe first) {
        
        return CraftTweakerRegistry.getHandlerFor(second).doesConflict(manager, second, first);
    }
    
}
