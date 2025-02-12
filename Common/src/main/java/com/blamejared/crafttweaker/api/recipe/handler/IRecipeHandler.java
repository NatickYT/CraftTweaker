package com.blamejared.crafttweaker.api.recipe.handler;


import com.blamejared.crafttweaker.api.recipe.manager.base.IRecipeManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.Function;

/**
 * Represents a handler for a specific type of recipe indicated by the generic parameter.
 *
 * <p>Differently from {@link IRecipeManager}, there can be more than one handler for recipe type, since handlers are
 * bound to the actual class type of the recipe in question (e.g. {@code ShapelessRecipe.class}, not
 * {@code minecraft:crafting_shapeless}).</p>
 *
 * <p>A recipe handler is responsible for recipe-class-specific behavior, documented in the following table:</p>
 *
 * <table>
 *     <thead>
 *         <tr>
 *             <th>Behavior</th>
 *             <th>Description</th>
 *             <th>Method Responsible</th>
 *         </tr>
 *     </thead>
 *     <tbody>
 *         <tr>
 *             <td><strong>Recipe Dumping</strong></td>
 *             <td>
 *                 Following a dump command, a recipe may need to be converted into a string that represents how that
 *                 same recipe can be added via a CraftTweaker script.
 *             </td>
 *             <td>{@link IRecipeHandler#dumpToCommandString(IRecipeManager, Recipe)}</td>
 *         </tr>
 *         <tr>
 *             <td><strong>Ingredient Replacement</strong></td>
 *             <td>
 *                 Following script method calls, a recipe may need to be replaced with an equivalent one, albeit with
 *                 some ingredients replaced with others according to certain {@link IReplacementRule}s.
 *             </td>
 *             <td>{@link IRecipeHandler#replaceIngredients(IRecipeManager, Recipe, List)}</td>
 *         </tr>
 *     </tbody>
 * </table>
 *
 * @param <T> The generic type the recipe handler can receive. Refer to the implementation specifications for more
 *            information.
 *
 * @implSpec Implementations of this interface will be discovered via classpath scanning for the {@link For} annotation.
 * The generic specialization of the implementation should match the one specified in {@link For#value()} for classes
 * annotated with a single annotation (e.g., a class annotated with {@code @For(MyRecipe.class)} should implement
 * {@code IRecipeHandler<MyRecipe>}). Implementations annotated with more than one annotation should instead specialize
 * with the closest super-class possible that allows them to correctly elaborate all of the instances (e.g., consider
 * two classes {@code Foo} and {@code Bar}, both extending {@code Baz}; a class annotated with both
 * {@code @For(Foo.class)} and {@code @For(Bar.class)} should implement {@code IRecipeHandler<Baz>}). Classes annotated
 * with {@code @For(IRecipe.class)} will be ignored.
 */
public interface IRecipeHandler<T extends Recipe<?>> {
    
    /**
     * Annotates a {@link IRecipeHandler} indicating which recipe classes it is able to handle.
     *
     * <p>This annotation is {@link Repeatable}.</p>
     *
     * @see IRecipeHandler
     */
    @Documented
    @Repeatable(For.Container.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface For {
        
        /**
         * Container for the {@link For} annotation.
         *
         * @see For
         * @see IRecipeHandler
         */
        @Documented
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.TYPE)
        @interface Container {
            
            /**
             * The container for the repetitions of the {@link For} annotation.
             *
             * @return An array containing all {@link For} instances.
             */
            For[] value();
            
        }
        
        /**
         * Indicates the recipe class the annotated {@link IRecipeHandler} is able to recognize and subsequently handle.
         *
         * @return The recipe class handled by this handler.
         *
         * @see IRecipeHandler
         */
        Class<? extends Recipe<?>> value();
        
    }
    
    /**
     * Exception that indicates that the current recipe handler does not support replacing for the targeted recipe
     * class.
     *
     * <p>Refer to {@link IRecipeHandler#replaceIngredients(IRecipeManager, Recipe, List)} for more information
     * regarding the exact semantics of this exception.</p>
     */
    class ReplacementNotSupportedException extends Exception {
        
        /**
         * Constructs a new exception with the specified detail message.
         *
         * <p>The cause is not initialized, and may subsequently be initialized by a call to {@link #initCause}.</p>
         *
         * @param message The detail message, which is saved for later retrieval by the {@link #getMessage()} method.
         */
        public ReplacementNotSupportedException(final String message) {
            
            super(message);
        }
        
        /**
         * Constructs a new exception with the specified detail message and cause.
         *
         * <p>Note that the detail message associated with {@code cause} is <em>not</em> automatically incorporated in
         * this exception's detail message.</p>
         *
         * @param message The detail message, which is saved for later retrieval by the {@link #getMessage()} method.
         * @param cause   The cause, which is saved for later retrieval by the {@link #getCause()} method. {@code null}
         *                is allowed and indicates that the cause is not available or not known.
         */
        public ReplacementNotSupportedException(final String message, final Throwable cause) {
            
            super(message, cause);
        }
        
    }
    
    /**
     * Attempts replacing the ingredient given as an argument according to the specified {@link IReplacementRule}s and
     * type.
     *
     * <p>The rules are applied one after the other in the same order as they are given in the {@code rules} list.</p>
     *
     * <p>The result of each rule application is considered as the new ingredient, which will be passed to the upcoming
     * rule. Effectively, this creates a chain of calls in the form of {@code ...(rule3(rule2(rule1(ingredient))))...}
     * which ensures that all rules always act on the most up-to-date representation of the current ingredient.</p>
     *
     * <p>The value of {@code type} represents the class type of the ingredient. Its value should be the most general
     * class type possible that the recipe can accept (e.g., a recipe that can accept any form of ingredient would
     * specify either {@code IIngredient} or {@code Ingredient} as the value for {@code type}).</p>
     *
     * @param ingredient The ingredient that should undergo replacement.
     * @param type       The actual class type of the ingredient, or one of its superclasses, as determined by the client.
     * @param recipe     The recipe whose ingredients are currently undergoing replacement; or {@code null} if no valid
     *                   recipe can be provided.
     * @param rules      A series of {@link IReplacementRule}s in the order they should be applied.
     * @param <S>        The type of the recipe whose ingredients are currently undergoing replacement. The given type must be
     *                   a subtype of {@link Recipe}. If no valid type exists, then {@link Recipe} is assumed.
     * @param <U>        The type of the ingredient that should undergo replacement. No restrictions are placed on the type of
     *                   the ingredient.
     *
     * @return An {@link Optional} holding the replaced ingredient, if any replacements have been carried out. If no
     * replacement rule affected the current ingredient, the return value should be {@link Optional#empty()}. It is
     * customary, though not required, that the value wrapped by the optional is a completely different object from
     * {@code ingredient} (i.e. {@code ingredient != result.get()}).
     */
    static <S extends Recipe<?>, U> Optional<U> attemptReplacing(final U ingredient, final Class<U> type, final S recipe, final List<IReplacementRule> rules) {
        
        final BinaryOperator<Optional<U>> combiner = (oldOpt, newOpt) -> newOpt.isPresent() ? newOpt : oldOpt;
        return rules.stream()
                .reduce(
                        Optional.empty(),
                        (optional, rule) -> combiner.apply(optional, rule.getReplacement(optional.orElse(ingredient), type, recipe)),
                        combiner
                );
    }
    
    /**
     * Creates a String representation of a valid {@code addRecipe} (or alternative) call for the given subclass of
     * {@link Recipe}.
     *
     * <p>Recipe dumps are triggered by the {@code /ct recipes} or {@code /ct recipes hand} commands.</p>
     *
     * <p>All newlines added to either the start or the end of the string will be automatically trimmed.</p>
     *
     * @param manager The recipe manager responsible for this kind of recipes.
     * @param recipe  The recipe that is currently being dumped.
     *
     * @return A String representing a {@code addRecipe} (or similar) call.
     */
    String dumpToCommandString(final IRecipeManager manager, final T recipe);
    
    /**
     * Handles the replacement of ingredients according to the given set of {@link IReplacementRule}s for the given
     * subclass of {@link Recipe}.
     *
     * <p>This method should try to apply all of the applicable rules to the recipe. If one of the rules fails to apply,
     * an error message should be generated via
     * {@link com.blamejared.crafttweaker.api.CraftTweakerAPI#LOGGER}. Incomplete application of
     * the replacement rules may or may not apply depending on the specific implementation: no specific contracts are
     * enforced by this method.</p>
     *
     * <p>If a particular recipe handler does not support replacement, a {@link ReplacementNotSupportedException} should
     * be raised, along with a helpful error message. A recipe handler <strong>must</strong> be consistent, meaning that
     * given the same recipe class, the behavior should be consistent: either an exception gets thrown or the
     * replacement gets carried out.</p>
     *
     * @param manager The recipe manager responsible for this kind of recipes.
     * @param recipe  The recipe whose ingredients should be replaced.
     * @param rules   A series of {@link IReplacementRule}s in the order they should be applied. Implementations are
     *                nevertheless allowed to reorder these rules as they see fit. Refer to the implementation
     *                specifications for more details.
     *
     * @return An {@link Optional} containing a function that creates the replaced recipe, if any replacements have been
     * carried out. If no replacement rule affected the current recipe, the return value should be
     * {@link Optional#empty()}. The parameter of the function will be the new ID of the recipe that should be used, as
     * determined by the method caller: the name may correspond to the old one or be a completely new one,
     * implementations are not allowed to make any assumptions on the value of this parameter. It is customary, though
     * not required, that the value returned by the wrapped function is a completely different object from
     * {@code recipe} (i.e. {@code recipe != result.get().apply(recipe.getId())}).
     *
     * @throws ReplacementNotSupportedException If the current handler does not support replacing for the given recipe
     *                                          class.
     * @implSpec The {@code rules} list not only indicates the {@link IReplacementRule}s that should be applied, but also
     * the order in which these should be applied. In other words, the rule at position {@code 0} should be applied to
     * the {@code recipe} before the rule in position {@code 1}. <strong>However</strong>, implementations are free to
     * ignore this detail and reorder the rule application to optimize certain applications if needed. This reordering
     * <strong>must</strong> guarantee that the resulting recipe behaves exactly as if the replacements were carried out
     * in order.
     * @implNote By default, this method throws a {@link ReplacementNotSupportedException}.
     */
    default Optional<Function<ResourceLocation, T>> replaceIngredients(final IRecipeManager manager, final T recipe, final List<IReplacementRule> rules)
            throws ReplacementNotSupportedException {
        
        throw new ReplacementNotSupportedException("Replacement is not supported for the recipe class '" + recipe.getClass()
                .getName() + "' with manager " + manager.getCommandString());
    }
    
    /**
     * Checks if the two recipes conflict with each other.
     *
     * <p>In this case, a conflict is defined as the two recipes being made in the exact same way (e.g. with the same
     * shape and the same ingredients if the two recipes are shaped crafting table ones).</p>
     *
     * <p>Conflicts are also considered symmetrical in this implementation, which means that if {@code firstRecipe}
     * conflicts with {@code secondRecipe}, the opposite is also true.</p>
     *
     * @param manager The recipe manager responsible for this kind of recipes.
     * @param firstRecipe The recipe which should be checked for conflict.
     * @param secondRecipe The other recipe which {@code firstRecipe} should be checked against. The recipe may or may
     *                     not be of the same type of {@code firstRecipe}. See the API note section for more details.
     * @param <U> The type of {@code secondRecipe}.
     * @return Whether the {@code firstRecipe} conflicts with {@code secondRecipe} or not.
     *
     * @apiNote The reason for which {@code secondRecipe} is specified as simply {@link Recipe} instead of as the
     * generic parameter {@code T} is to allow more flexibility in the conflict checking. In fact, this choice allows
     * for checking to also occur between different types of recipes (e.g. shaped vs shapeless crafting table recipes),
     * allowing for a broader range of checking. Nevertheless, the two recipes are <strong>ensured</strong> to be of the
     * same {@link net.minecraft.world.item.crafting.RecipeType recipe type} (i.e.
     * {@code firstRecipe.getType() == secondRecipe.getType()}).
     *
     * @implNote By default, this method returns {@code false}.
     */
    default <U extends Recipe<?>> boolean doesConflict(final IRecipeManager manager, final T firstRecipe, final U secondRecipe) {
        
        return false;
    }
    
}
