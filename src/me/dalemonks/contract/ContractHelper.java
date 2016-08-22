/*
 * University of Central Lancashire
 * CO3808 - Double Project
 * Supervisor - Chris Casey
 * Project - Java Contracts
 */
package me.dalemonks.contract;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import me.dalemonks.contract.annotations.method.Invariant;
import me.dalemonks.contract.annotations.variables.Var;

/**
 *
 * @author Dale Monks
 */
class ContractHelper {

    private static final int STACKTRACEPRE = 6; //This is the number of items to be removed from the stack trace.
    private static final int STACKTRACEINSTANSIATE = 2; //This is the number of items to be removed from the stack trace.

    /**
     * This method is used within the method handler to clone an object using
     * reflection.
     *
     * @param toClone is the object which is going to be cloned.
     * @return is the cloned object or the original object if it can't be.
     */
    public static Object cloneObject(Object toClone) {
        try {
            //This checks that to clone is not a collection or a map.
            if (!(toClone instanceof Map<?, ?> || toClone instanceof Collection<?>)) {
                Object clone = null;
                //This checks that it is not an array.
                if (toClone.getClass().isArray()) {
                    clone = cloneArray(toClone); //This clones the array using a helper method.
                } else {
                    clone = toClone.getClass().newInstance();
                    for (Field field : toClone.getClass().getDeclaredFields()) { //This checks all the fields
                        field.setAccessible(true); //This allows the fields to be accessed. 
                        if (field.get(toClone) == null || Modifier.isFinal(field.getModifiers())) { //This checks if the object field is null or final to which it skips it.
                            continue;
                        }
                        if (field.getType().isPrimitive() || field.getType().equals(String.class) //This checks if the type is not an object
                                || field.getType().getSuperclass().equals(Number.class) //which had been made.
                                || field.getType().equals(Boolean.class)) {
                            field.set(clone, field.get(toClone)); //This simply sets the value to it, if it's not complex object.
                        } else {
                            Object childObj = field.get(toClone); //The child object is created, or attempted.

                            if (childObj == toClone) { //This sets the object. 
                                field.set(clone, clone); //If it's a self reference itself.
                            } else {
                                field.set(clone, cloneObject(field.get(toClone))); //This uses recursion to set any child objects. 
                            }
                        }
                    }
                }
                return clone; //If possible it returns the clone.
            }
            if (toClone instanceof Collection<?>) {
                Class<?> myClass = toClone.getClass(); //This gets the class of the collection.

                Object[] tempArr = ((Collection<?>) toClone).toArray(); //This sets it to an array.

                Object clonedArray = cloneArray(tempArr); //This clones the array.

                Collection<Object> tempCollection = ((Collection<Object>) myClass.newInstance()); //This gets a new instnace of a collection.                
                for (int i = 0; i < tempArr.length; i++) { //This loops through the array.
                    tempCollection.add(Array.get(clonedArray, i)); //This loops through the array and adds it to the collection.
                }
                return tempCollection; //this returns the collection made.
            }
            return toClone;
        } catch (InstantiationException | IllegalAccessException | SecurityException | IllegalArgumentException e) {
            return toClone; //This returns null if it's not possible to create the object for whatever reason.
        }
    }

    /**
     * Private helper function for cloning an array.
     *
     * @param toClone is the array to be cloned.
     * @return is a copy of the array.
     */
    private static Object cloneArray(Object toClone) {
        Object[] tempArray = new Object[Array.getLength(toClone)]; //This creates a new array

        if (toClone.getClass().getComponentType().isPrimitive()) { //Primitive types must be copied using =
            for (int i = 0; i < tempArray.length; i++) { //Loops through the array
                tempArray[i] = (Object) Array.get(toClone, i); //Casts them into an object and puts it into the array.
            }
        } else { //If it's not primitive then it uses the method for cloning.
            for (int i = 0; i < tempArray.length; i++) { //This loops through the array.
                tempArray[i] = (Object) cloneObject(Array.get(toClone, i)); //Clones it using reflection and recurssion. 
            }
        }
        return tempArray; //Returns the copy.
    }

    /**
     * This function alters a map to change the keys so that they have old in
     * front of their key.
     *
     * @param map is the map which is going to change the keys
     * @return is the changed map with old in all the keys.
     */
    public static HashMap<String, Object> alterOldMap(HashMap<String, Object> map) {
        HashMap<String, Object> tmp = new HashMap<>();

        map.keySet().stream().forEach((s) -> { //This stream loops around the map.
            tmp.put("old" + s, map.get(s)); //Adding and altering the key from the old map.
        });

        return tmp; //Returning the new created map.
    }

    /**
     * This is a helper method which creates a hash map from the argument values
     * and the parameter methods.
     *
     * @param args is the argument values.
     * @param params is the parameters for a method.
     * @return is a hash map with assigned parameter names and values.
     */
    public static HashMap<String, Object> mapHelper(Object[] args, Parameter[] params) {
        HashMap<String, Object> map = new HashMap<>(); //Creats an instance of a hashmap.

        for (int i = 0; i < params.length; i++) { //This loops through the parameters.
            Object o = cloneObject(args[i]); //This clones the object using reflection.

            map.put(params[i].getAnnotation(Var.class).value(), o);
        }
        return map; //This returns the map created.
    }

    /**
     * This is a helper function to get the fields.
     *
     * @param obj is the object where the
     * @return is a hash map of all the named fields within the object.
     */
    public static HashMap<String, Object> fieldHelper(Object obj, Field[] fields) {
        HashMap<String, Object> map = new HashMap<>();

        for (Field field : fields) {
            try {
                //This checks all the fields
                field.setAccessible(true); //This allows the fields to be accessed.
                Var ann = field.getAnnotation(Var.class);
                if (ann == null) {
                    continue;
                }

                Object value = field.get(obj); //This is the fields actual value

                if (value == null || Modifier.isFinal(field.getModifiers())) { //This checks if the object field is null or final to which it skips it.
                    continue;
                }

                Object fieldClone = cloneObject(value); //This clones the object.
                map.put(ann.value(), fieldClone);

            } catch (IllegalArgumentException | IllegalAccessException ex) {
                //This catches if any errors occur preventing the field being read.
            }
        }
        return map;
    }

    /**
     * This alters the stack trace to be more understandable for instantiation.
     *
     * @param stack is the initial stack trace.
     * @return is the alter stack trace.
     */
    public static StackTraceElement[] alterStackInstantiation(StackTraceElement[] stack) {
        StackTraceElement[] newStack = new StackTraceElement[stack.length - STACKTRACEINSTANSIATE]; //This removes all stack info about this class

        for (int i = STACKTRACEINSTANSIATE; i < stack.length; i++) { //This loops through the stacks trace.
            newStack[i - STACKTRACEINSTANSIATE] = stack[i]; //This then sets the new stack trace without the trace for the contract checker.
        }
        return newStack;
    }

    /**
     * This alters the stack trace to be more understandable by removing all
     * info relating to the creation and use of this class.
     *
     * @param stack is the initial stack trace.
     * @return is the alter stack trace.
     */
    public static StackTraceElement[] alterStackWithinContract(StackTraceElement[] stack) {
        StackTraceElement[] newStack = new StackTraceElement[stack.length - STACKTRACEPRE]; //This removes all stack info about this class

        for (int i = STACKTRACEPRE; i < stack.length; i++) { //This loops through the stacks trace.
            newStack[i - STACKTRACEPRE] = stack[i]; //This then sets the new stack trace without the trace for the contract checker.
        }
        return newStack;
    }

    /**
     * This alters the stack trace for any thrown exceptions from the method.
     *
     * @param initialTrace is the initial trace.
     * @return is the altered trace again removing any un-needed info.
     */
    public static StackTraceElement[] alterTrace(StackTraceElement[] initialTrace) {
        List<StackTraceElement> newTrace = new ArrayList<>(Arrays.asList(initialTrace));

        int removePos = 0;
        for (removePos = 0; removePos < newTrace.size(); removePos++) { //This finds where the contract constructor is.
            if (newTrace.get(removePos).getFileName().contains("ContractConstructor")) { //within the trace.
                break;
            }
        }

        int lowerLimit = removePos - 5; //These set the lower and uper limits for altering.
        int upperLimit = removePos + 3;

        List<StackTraceElement> tmp = new ArrayList<>(newTrace.subList(0, lowerLimit)); //This 
        tmp.addAll(newTrace.subList(upperLimit, newTrace.size()));

        newTrace = tmp;

        return Arrays.copyOf(newTrace.toArray(), newTrace.size(), StackTraceElement[].class);
    }

    /**
     * This is a simpler helper to check all the fields check if any have an
     * invariant.
     *
     * @param fields are the fields of the class.
     * @return is whether any field has an invariant.
     */
    public static boolean fieldHasInvariant(Field[] fields) {
        for (Field field : fields) { //Loops through all fields.
            //Check if there's no annotations with invariants.
            if (field.getAnnotationsByType(Invariant.class).length != 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * This method is used to get an object using constructors and trying each
     * one.
     *
     * @param <T> is the type of the class.
     * @param clas is the class that's being instantiated.
     * @param args are the arguments being checked.
     * @return is null or an instance of the class.
     */
    public static <T> T getConstructor(Class<T> clas, Object[] args) {
        Constructor[] cons = clas.getConstructors(); //Gets all constructors 

        T found = null;
        for (Constructor con : cons) { //Loops through each
            try {
                found = clas.cast(con.newInstance(args)); //Attempts to get an instance using the args
                return found; //Returns an instance if possible
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                //This catches any exceptions about creating the class.
            }
        }
        return found;
    }
}
