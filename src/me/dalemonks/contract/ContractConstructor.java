/*
 * University of Central Lancashire
 * CO3808 - Double Project
 * Supervisor - Chris Casey
 * Project - Java Contracts
 */
package me.dalemonks.contract;

import java.lang.reflect.Array;
import me.dalemonks.contract.annotations.method.Pre;
import me.dalemonks.contract.annotations.method.Post;
import me.dalemonks.contract.annotations.method.Invariant;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.Proxy;
import javassist.util.proxy.ProxyFactory;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import me.dalemonks.contract.annotations.method.Exists;
import me.dalemonks.contract.annotations.method.ForAll;
import me.dalemonks.contract.annotations.method.PostThrow;
import me.dalemonks.contract.exceptions.ContractException;

/**
 *
 * @author Dale Monks
 */
public class ContractConstructor {

    private final static ScriptEngineManager manager = new ScriptEngineManager();
    private final static ScriptEngine engine = manager.getEngineByName("js");    //This gets an engine from the manager using java script.

    /**
     * This method is used to create an object which will test contracts If any
     * are true an assertion is thrown on calling a method.
     *
     * @param <T> is the type of the class.
     * @param clas is the class which will have an instantiated.
     * @param arguments are the arguments for the constructor.
     * @return is the object with the contract hooked in.
     * @throws InstantiationException is thrown if the class cannot be
     * instantiated.
     */
    public static <T> T createObject(Class<T> clas, Object[] arguments) throws InstantiationException {
        ProxyFactory f = new ProxyFactory(); //This starts a proxy factory which will create the proxy.
        f.setSuperclass(clas); //This sets the super class.

        Field[] fields = clas.getDeclaredFields();  //Get all the fields from the class it's being made to replicate
        boolean hasFieldInv = ContractHelper.fieldHasInvariant(fields);
        //The is to ensure that a class which has a field invariant 
        //then all of the methods will be checked.

        f.setFilter((Method m) -> {
            //This checks if any annotations are present for a method supplied.
            return m.getAnnotationsByType(Pre.class).length != 0
                    || m.getAnnotationsByType(Post.class).length != 0
                    || m.getAnnotationsByType(Invariant.class).length != 0
                    || m.getAnnotationsByType(ForAll.class).length != 0
                    || m.getAnnotationsByType(Exists.class).length != 0
                    || m.getAnnotationsByType(PostThrow.class).length != 0
                    || hasFieldInv;
        });

        Class c = f.createClass();  //This then creates a new class from the proxy factory.

        MethodHandler mi = (Object self, Method m, Method proceed, Object[] args) -> { //This is the method handler for the proxy created.
            Parameter[] params = m.getParameters(); //This gets the parameters of the method.
            //These are maps of all the parameters and fields to be checked.
            HashMap<String, Object> initialParameters = ContractHelper.mapHelper(args, params); //This uses a helper to assign the parameter names and values.
            HashMap<String, Object> afterParameters = initialParameters; //This sets the after parameters to the intial to begin with.
            HashMap<String, Object> initialFields = ContractHelper.fieldHelper(self, fields); //This uses a helper to assign the field name and values
            HashMap<String, Object> afterFields = initialFields; //This sets the after fields to the intial to begin.
            //These are arrays of all the annotations.
            Pre[] preArr = m.getAnnotationsByType(Pre.class); //This gets all the annotations that could be on the methods.
            Post[] postArr = m.getAnnotationsByType(Post.class);
            Invariant[] invArr = m.getAnnotationsByType(Invariant.class);
            ForAll[] forAllArr = m.getAnnotationsByType(ForAll.class);
            Exists[] existsArr = m.getAnnotationsByType(Exists.class);

            invArr = getFieldInvs(invArr, fields);

            for (Pre pre : preArr) { //This loops through all annotations for pre.
                preCheck(pre.value(), initialParameters, initialFields); //This then checks the pre conditions.
            }

            for (Invariant inv : invArr) {
                invCheck(inv.value(), initialParameters, initialFields); //This then checks the invariant condition.
            }

            Object result = null; //This intialised the result to null.

            try {
                result = proceed.invoke(self, args);  // execute the original method.

                afterParameters = ContractHelper.mapHelper(args, params); //This gets the parameters after the method is called.
                afterFields = ContractHelper.fieldHelper(self, fields); //This gets the fields after

                initialParameters = ContractHelper.alterOldMap(initialParameters);
                initialFields = ContractHelper.alterOldMap(initialFields);

                for (Post post : postArr) {
                    postCheck(post.value(), initialParameters, afterParameters, initialFields, afterFields, result); //This then runs any post checks.
                }
                
                for (ForAll forAll : forAllArr) {
                    forAllCheck(forAll.value(), initialParameters, afterParameters, initialFields, afterFields, result);
                }

                for (Exists exist : existsArr) {
                    existsCheck(exist.value(), initialParameters, afterParameters, initialFields, afterFields, result);
                }
                
                for (Invariant inv : invArr) {
                    invCheck(inv.value(), afterParameters, afterFields);
                }
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException thrownByMethod) {
                Throwable cause = thrownByMethod.getCause();

                if (!(cause instanceof AssertionError || cause instanceof ContractException)) {
                    if (cause != null) { //If cause is null then it is not an exception from the method.
                        PostThrow[] thrown = m.getAnnotationsByType(PostThrow.class);

                        for (PostThrow post : thrown) {
                            if (cause.getClass().equals(post.exception())) { //Check if it has exception to check.
                                postThrowCheck(post.condition(), initialParameters, afterParameters, initialFields, afterFields, result, cause); //This then runs any post checks.
                            }
                        }
                        cause.setStackTrace(ContractHelper.alterTrace(cause.getStackTrace())); //This sets the trace of the throwable   
                        throw cause;
                    }
                }
                throw thrownByMethod;
            }
            return result; // this returns the result of the method invocation.
        };

        Object obj = ContractHelper.getConstructor(c, arguments); //This uses a helper to get a constructor.

        //If it is still null then it can't instantiated by reflection.
        if (obj == null) {
            InstantiationException e = new InstantiationException("Class could not be instantiated: " + clas);
            e.setStackTrace(ContractHelper.alterStackInstantiation(e.getStackTrace()));
            throw e;
        }

        ((Proxy) obj).setHandler(mi); //This then sets it's handler using the proxy.

        return clas.cast(obj); //This returns the object which should now have the proxy with it.
    }

    /**
     * This creates a contracted object using the default constructor only.
     *
     * @param <T> is the type of the class.
     * @param clas is the class which will have an instantiated.
     * @return is the object with the contract hooked in.
     * @throws InstantiationException is thrown if the class cannot be
     * instantiated.
     */
    public static <T> T createObject(Class<T> clas) throws InstantiationException {
        return createObject(clas, null); //If there's no arguments then it should be null.
    }

    /**
     * This method gets the fields with invariants and adds them to the
     * invariant array.
     *
     * @param invArr are the invariants already found on the methods.
     * @param fields are the fields of the class.
     * @return is a new array with any field invariants.
     */
    private static Invariant[] getFieldInvs(Invariant[] invArr, Field[] fields) {
        List<Invariant> list = new ArrayList<>(Arrays.asList(invArr)); //Creates a list.
        for (Field field : fields) { //Loops through all the fields
            Invariant[] fieldInvs = field.getAnnotationsByType(Invariant.class);
            if (fieldInvs.length != 0) { //Makes sure it has invariants
                list.addAll(Arrays.asList(fieldInvs)); //Addss all invariants
            }
        }
        Invariant[] tmp = Arrays.copyOf(list.toArray(), list.size(), Invariant[].class); //Converts the list to an array of invariant type.
        return tmp;
    }

    /**
     * This method is used by the method handler to check pre-conditions.
     *
     * @param condition is the string value of the pre-condition.
     * @param params is the parameters and values for the method.
     * @throws ContractException is the exception thrown when an error is found
     * with a contract
     */
    private static void preCheck(String condition, HashMap<String, Object> params, HashMap<String, Object> fields) throws ContractException {
        Bindings bind = engine.createBindings(); //A binding is used to link java and javascript.

        bind.putAll(params); //This adds the hashmaps of parameters to the engine.
        bind.putAll(fields); //This adds the fields.

        engine.setBindings(bind, ScriptContext.ENGINE_SCOPE); //This binds all the parameters to the engine.

        testSimpleAssertion(condition, "Pre condition"); //This then tests the condition.
    }

    /**
     * This method is used by the method handler to check post-conditions.
     *
     * @param condition is the condition to be checked.
     * @param preParams are the pre-invocation parameters.
     * @param postParams are the post-invocation parameters.
     * @param result is the result of the method being checked.
     * @throws ContractException is the exception thrown when an error is found
     * with a contract
     */
    private static void postCheck(String condition, HashMap<String, Object> preParams, HashMap<String, Object> postParams, HashMap<String, Object> preFields, HashMap<String, Object> postFields, Object result) throws ContractException {
        Bindings bind = engine.createBindings(); //A binding is used to link java and javascript.

        bind.putAll(preParams); //This adds the hashmaps of parameters to the engine.
        bind.putAll(postParams);
        bind.putAll(preFields); //This adds the fields.
        bind.putAll(postFields);

        bind.put("result", result); //This adds the result to the script engine.

        engine.setBindings(bind, ScriptContext.ENGINE_SCOPE);

        testSimpleAssertion(condition, "Post condition"); //This runs the test.
    }

    //Exception checking.
    private static void postThrowCheck(String condition, HashMap<String, Object> preParams, HashMap<String, Object> postParams, HashMap<String, Object> preFields, HashMap<String, Object> postFields, Object result, Throwable e) throws ContractException {
        Bindings bind = engine.createBindings(); //A binding is used to link java and javascript.

        bind.putAll(preParams); //This adds the hashmaps of parameters to the engine.
        bind.putAll(postParams);
        bind.putAll(preFields); //This adds the fields.
        bind.putAll(postFields);

        bind.put("result", result); //This adds the result to the script engine.

        engine.setBindings(bind, ScriptContext.ENGINE_SCOPE);

        testSimpleAssertion(condition, "Post Thrown - Exception: \n{" + e.getClass().getName() + "}"); //This runs the test.
    }

    /**
     * This method is used by the method handler to check the invariant. Whilst
     * being able to use the old values is possible, in terms of design it was
     * taken out of initial design.
     *
     * @param condition is the condition to be checked.
     * @param params are the parameters.
     * @param fields are the fields of the object.
     * @throws ContractException is the exception thrown when an error is found
     * with a contract
     */
    private static void invCheck(String condition, HashMap<String, Object> params, HashMap<String, Object> fields) throws ContractException {
        Bindings bind = engine.createBindings(); //A binding is used to link java and javascript.

        bind.putAll(params); //This adds the hashmaps of parameters to the engine.
        bind.putAll(fields); //This adds the fields.

        engine.setBindings(bind, ScriptContext.ENGINE_SCOPE); //This binds all the parameters to the engine.

        testSimpleAssertion(condition, "Invariant condition"); //This tests the script.
    }

    /**
     * This method is for checking the for all condition.
     *
     * @param condition is the string which will build the script.
     * @param preParams are the pre execution parameters.
     * @param postParams are the post execution parameter.
     * @param preFields are the pre execution fields.
     * @param postFields are the post execution fields.
     * @param result is the result from the method.
     * @throws ContractException is thrown when the contract is invalid.
     */
    private static void forAllCheck(String condition, HashMap<String, Object> preParams, HashMap<String, Object> postParams, HashMap<String, Object> preFields, HashMap<String, Object> postFields, Object result) throws ContractException {
        //This is the generalised looped post check, which changes depends on who called it.
        loopedCheck(condition, preParams, postParams, preFields, postFields, result, false, "ForAll(Post)");
    }

    /**
     * This method is for checking the exists condition.
     *
     * @param condition is the string which will build the script.
     * @param preParams are the pre execution parameters.
     * @param postParams are the post execution parameter.
     * @param preFields are the pre execution fields.
     * @param postFields are the post execution fields.
     * @param result is the result from the method.
     * @throws ContractException is thrown when the contract is invalid.
     */
    private static void existsCheck(String condition, HashMap<String, Object> preParams, HashMap<String, Object> postParams, HashMap<String, Object> preFields, HashMap<String, Object> postFields, Object result) throws ContractException {
        //This is the generalised loop.
        loopedCheck(condition, preParams, postParams, preFields, postFields, result, true, "Exists(Post)");
    }

    /**
     * This method is the generalised loop checker.
     *
     * @param condition is the string which will build the script.
     * @param preParams are the pre execution parameters.
     * @param postParams are the post execution parameter.
     * @param preFields are the pre execution fields.
     * @param postFields are the post execution fields.
     * @param result is the result from the method.
     * @param assertValue is the initial value of assert.
     * @param errorMessage is the error message for the loop.
     * @throws ContractException is thrown when the contract is invalid.
     */
    private static void loopedCheck(String condition, HashMap<String, Object> preParams, HashMap<String, Object> postParams, HashMap<String, Object> preFields, HashMap<String, Object> postFields, Object result, boolean assertValue, String errorMessage) throws ContractException {
        Bindings bind = engine.createBindings();
        //This creates and binds all the variables.
        bind.putAll(preParams);
        bind.putAll(postParams);
        bind.putAll(preFields);
        bind.putAll(postFields);
        bind.put("result", result);

        String collectionName;
        String[] tokenized = condition.split(",\\s+"); //This tokenizes acording to ", " similar to a parameter

        collectionName = tokenized[0]; //The first value is the collection name.

        Object collectionObj = postParams.get(collectionName);
        Collection<?> collection = null;
        if (collectionObj instanceof Collection<?>) {
            collection = (Collection<?>) collectionObj;
        }

        if (collection == null) { //If there's no collection found it will be null.
            collectionObj = postFields.get(collectionName); //This checks post fields
            if (collectionObj instanceof Collection<?>) {
                collection = (Collection<?>) collectionObj;
            }
        }
        if (collection == null) {
            collection = (Collection<?>) result; //Check incase result is the collection.
        }

        engine.setBindings(bind, ScriptContext.ENGINE_SCOPE); //Sets bindings to the engine.ss

        String start = "0"; //This sets the default values
        String end = "-1";
        
        if (collection != null) {
            end = String.valueOf(collection.size()); //This remains as 0 until the collection is known or a value is entered.
        } else if (collectionObj != null && collectionObj.getClass().isArray()) {
            end = String.valueOf(Array.getLength(collectionObj));
        }
        String newCondition = "";

        int length = tokenized.length;

        newCondition = tokenized[length - 1]; //The condition is always the last thing.

        if (length == 4) { //If there's 2 extra parameters then use these as limits.
            start = tokenized[length - 3];
            end = tokenized[length - 2];
        }

        String script = "var assert =" + String.valueOf(assertValue) + "; ";
        script += "var i = 0;" //This is the basic loop with values inserted.
                + "for(i = " + start + "; i < " + end + "; i++){ "
                + "if(" + ((!assertValue) ? "!(" : "(") + newCondition + "))"
                + "{ assert = !assert; break;}"
                + "}";

        getAssert(script, errorMessage, newCondition); //This then runs the script.
    }

    /**
     * This method is used to test a condition from a condition.
     *
     * @param condition is the condition which will be checked.
     * @param sourceMessage
     * @throws ContractException
     */
    private static void testSimpleAssertion(String condition, String sourceMessage) throws ContractException {
        String script = "var assert = !("; //This sets the condition to the assert varriable.

        script += condition + ");";

        getAssert(script, sourceMessage, condition); //This runs the script created and checks it.
    }

    /**
     * This method runs and checks a scrip with the variable assert within it.
     *
     * @param script is the script to be run by the engine.
     * @param sourceMessage is the message from the source incase of an error.
     * @param condition is the condition it is checking.
     * @throws ContractException is thrown when an error happens within the
     * contract.
     */
    private static void getAssert(String script, String sourceMessage, String condition) throws ContractException {
        try {
            engine.eval(script); //This runs the script.
        } catch (ScriptException ex) {
            throw new ContractException(ex.getMessage()); //Simply uses the scrip message. 
        }
        try {
            if ((boolean) engine.get("assert")) { //This checks if the assert is true.
                String[] stringArray = condition.split("(\\W|\\s)+"); //This splits it if it is a non letter or space.
                int length = stringArray.length;
                HashMap<String, Object> valuePairs = new HashMap<>();
                
                for (String name : stringArray) { //this loops through the array of possible variables.
                    if (name.length() == 0) {
                        continue;
                    }
                    Object temp = null;
                    try {
                        temp = engine.get(name); //Gets the engine variable.
                    } catch (NullPointerException e) {
                        //Throwaway e.
                    }

                    if (temp == null) { //This checks that temp is not null.
                        continue;
                    }
                    valuePairs.put(name, temp); //Adds the name if the variable is found.
                }

                String assertionError = "\nContract is violated - " + sourceMessage + ": " + condition; //This is formatting for the output.
                assertionError += "\n\nVariable Name\t Value";
                assertionError += "\n----------------------";

                
                Set<String> keySet = valuePairs.keySet();
                if(keySet.contains("i")){
                    Object iValue = valuePairs.get("i");
                    keySet.remove("i");
                    assertionError += formatForAssert("i", iValue, sourceMessage); 
                }
                //This loops through all the variables in the condition which have been found within the engine.        
                for (String name : valuePairs.keySet()) {
                    Object tempObj = valuePairs.get(name);
                    assertionError += formatForAssert(name, tempObj, sourceMessage); 
                }

                AssertionError e = new AssertionError(assertionError); //This throws an assertion exception if the condition is true.

                e.setStackTrace(ContractHelper.alterStackWithinContract(e.getStackTrace())); //This sets the trace and then throws the error.

                throw e;
            }
        } catch (NullPointerException | ClassCastException e) {
            ContractException contractEx = new ContractException("Contract doesn't evaluate to a boolean.");

            contractEx.setStackTrace(ContractHelper.alterStackWithinContract(contractEx.getStackTrace()));

            throw contractEx; //this is thrown if assert is null or not boolean.
        }
    }
    
     public static String formatForAssert(String name, Object obj, String sourceMessage) {
        String assertionError = "";
        if (name.length() > 16) { //Name formatting.
            name = name.substring(0, 13);
            name += "..."; //This cuts the name down incase it's too long for the formatting.
        }

        if (obj instanceof Collection<?>) { //This checks if ther object is a collection.
            int i = ((Double) engine.get("i")).intValue(); //This gets the location of i.
            if (sourceMessage.contains("ForAll")) { //A for all loop will output where it breaks.
                Object temp = ((Collection<?>) obj).toArray()[i]; //This converts the collection to an array and gets element i
                obj = temp; //Sets the value to the single value for the for all.
                if (name.length() >= 13) {
                    name = name.substring(0, 13); //This formats the name if it's too long 
                }
                name += "[i]"; //this adds it to make it seem like it's apart of the array.
            }
        }
        if (obj.getClass().isArray()) {
            int i = ((Double) engine.get("i")).intValue();
            if (sourceMessage.contains("ForAll")) { //A for all loop will output where it breaks.
                Object temp = Array.get(obj, i); //This gets the i'th element of the array.
                obj = temp; //Sets the value to the single value for the for all.
                if (name.length() >= 13) {
                    name = name.substring(0, 13); //This formats the name if it's too long 
                }
                name += "[i]"; //this adds it to make it seem like it's apart of the array.
            }
        }

        assertionError += "\n" + name; //This adds it to the error.
        //A tab is rougly 8 characters.
        for (int i = 0; i < (2 - (name.length() / 8)); i++) { //This then loops through to work out how many tabs to add
            assertionError += "\t "; //this adds a tab for every needed oen.
        }
        assertionError += obj;//this then adds the value
        
        return assertionError;
    }
}
