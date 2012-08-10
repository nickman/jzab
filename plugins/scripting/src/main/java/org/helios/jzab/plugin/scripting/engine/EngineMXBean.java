/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2007, Helios Development Group and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org. 
 *
 */
package org.helios.jzab.plugin.scripting.engine;

import java.util.List;

import javax.script.ScriptContext;
import javax.script.ScriptEngineFactory;



/**
 * <p>Title: EngineMXBean</p>
 * <p>Description: JMX interface for {@link Engine}, a light wrapper around {@link javax.script.ScriptEngineFactory}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>javax.script.EngineMXBean</code></p>
 */
public interface EngineMXBean {
	
	
	/**
	 * <p>Title: ThreadSafety</p>
	 * <p>Description: Enumerates and discovers the possible thread safety advisories for script engine factories</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.jzab.plugin.scripting.engine.EngineMXBean.ThreadSafety</code></p>
	 */
	public static enum ThreadSafety {
		/** The engine implementation is not thread safe, and cannot be used to execute scripts concurrently on multiple threads.  */
		UNSAFE,
		/** The engine implementation is internally thread-safe and scripts may execute concurrently although effects of script execution on one thread may be visible to scripts on other threads.  */
		MULTITHREADED,		
		/** The implementation satisfies the requirements of "MULTITHREADED", and also, the engine maintains independent values for symbols in scripts executing on different threads.  */
		THREADISOLATED,
		/** The implementation satisfies the requirements of "THREAD-ISOLATED". In addition, script executions do not alter the mappings in the Bindings which is the engine scope of the ScriptEngine. In particular, the keys in the Bindings and their associated values are the same before and after the execution of the script.  */
		STATELESS;
		
		/**
		 * Returns the thread safety advisory for the passed ScriptEngineFactory.
		 * @param sef The script engine factory
		 * @return The thread safety advisory
		 */
		public static ThreadSafety forValue(ScriptEngineFactory sef) {
			Object prop = sef.getParameter("THREADING");
			if(prop==null) return UNSAFE;
			return ThreadSafety.valueOf(prop.toString().trim().toUpperCase().replace("-", ""));
		}
	}
	
    /**
     * Returns the full  name of the <code>ScriptEngine</code>.  For
     * instance an implementation based on the Mozilla Rhino Javascript engine
     * might return <i>Rhino Mozilla Javascript Engine</i>.
     * @return The name of the engine implementation.
     */
    public String getEngineName();
    
    /**
     * Returns the version of the <code>ScriptEngine</code>.
     * @return The <code>ScriptEngine</code> implementation version.
     */
    public String getEngineVersion();
    
    
    /**
     * Returns an immutable list of filename extensions, which generally identify scripts
     * written in the language supported by this <code>ScriptEngine</code>.
     * The array is used by the <code>ScriptEngineManager</code> to implement its
     * <code>getEngineByExtension</code> method.
     * @return The list of extensions.
     */
    public List<String> getExtensions();
    
    
    /**
     * Returns an immutable list of mimetypes, associated with scripts that
     * can be executed by the engine.  The list is used by the
     * <code>ScriptEngineManager</code> class to implement its
     * <code>getEngineByMimetype</code> method.
     * @return The list of mime types.
     */
    public List<String> getMimeTypes();
    
    /**
     * Returns an immutable list of  short names for the <code>ScriptEngine</code>, which may be used to
     * identify the <code>ScriptEngine</code> by the <code>ScriptEngineManager</code>.
     * For instance, an implementation based on the Mozilla Rhino Javascript engine might
     * return list containing {&quot;javascript&quot;, &quot;rhino&quot;}.
     */
    public List<String> getNames();
    
    /**
     * Returns the name of the scripting langauge supported by this
     * <code>ScriptEngine</code>.
     * @return The name of the supported language.
     */
    public String getLanguageName();
    
    /**
     * Returns the version of the scripting language supported by this
     * <code>ScriptEngine</code>.
     * @return The version of the supported language.
     */
    public String getLanguageVersion();
    

    
    /**
     * Returns a String which can be used to invoke a method of a  Java object using the syntax
     * of the supported scripting language.  For instance, an implementaton for a Javascript
     * engine might be;
     * <p>
     * <code><pre>
     * public String getMethodCallSyntax(String obj,
     *                                   String m, String... args) {
     *      String ret = obj;
     *      ret += "." + m + "(";
     *      for (int i = 0; i < args.length; i++) {
     *          ret += args[i];
     *          if (i == args.length - 1) {
     *              ret += ")";
     *          } else {
     *              ret += ",";
     *          }
     *      }
     *      return ret;
     * }
     *</pre></code>
     * <p>
     *
     * @param obj The name representing the object whose method is to be invoked. The
     * name is the one used to create bindings using the <code>put</code> method of
     * <code>ScriptEngine</code>, the <code>put</code> method of an <code>ENGINE_SCOPE</code>
     * <code>Bindings</code>,or the <code>setAttribute</code> method
     * of <code>ScriptContext</code>.  The identifier used in scripts may be a decorated form of the
     * specified one.
     *
     * @param m The name of the method to invoke.
     * @param args names of the arguments in the method call.
     *
     * @return The String used to invoke the method in the syntax of the scripting language.
     */
    public String getMethodCallSyntax(String obj, String m, String... args);
    
    /**
     * Returns a String that can be used as a statement to display the specified String  using
     * the syntax of the supported scripting language.  For instance, the implementaton for a Perl
     * engine might be;
     * <p>
     * <pre><code>
     * public String getOutputStatement(String toDisplay) {
     *      return "print(" + toDisplay + ")";
     * }
     * </code></pre>
     *
     * @param toDisplay The String to be displayed by the returned statement.
     * @return The string used to display the String in the syntax of the scripting language.
     *
     *
     */
    public String getOutputStatement(String toDisplay);
    
    
    /**
     * Returns A valid scripting language executable progam with given statements.
     * For instance an implementation for a PHP engine might be:
     * <p>
     * <pre><code>
     * public String getProgram(String... statements) {
     *      $retval = "&lt;?\n";
     *      int len = statements.length;
     *      for (int i = 0; i < len; i++) {
     *          $retval += statements[i] + ";\n";
     *      }
     *      $retval += "?&gt;";
     *
     * }
     * </code></pre>
     *
     *  @param statements The statements to be executed.  May be return values of
     *  calls to the <code>getMethodCallSyntax</code> and <code>getOutputStatement</code> methods.
     *  @return The Program
     */
    
    public String getProgram(String... statements);
    
    /**
     * Returns A valid scripting language executable progam with given statements.
     * Convenience implementation for JConsole
     * @param delim The delimiter for the statements being passed
     * @param statements Delimited statements
     * @return The Program
     */
    public String getProgram(String delim, String statements);
    
    /**
     * Determines if scripts can be compiled by this engine
     * @return true if scripts can be compiled by this engine, false otherwise
     */
    public boolean isCompilable();
    
    /**
     * Determines if this engine supports the invocation of procedures in scripts that have previously been executed. 
     * @return true if the engine is invocable, false otherwise
     */
    public boolean isInvocable();
    
    /**
     * Returns the script engine factory's threading advisory
     * @return the script engine factory's threading advisory
     */
    public String getThreading();    
    
	/**
	 * Determines if the engine is not thread safe
	 * @return true if the engine is not thread safe, false if it is thread safe or better
	 */
	public boolean isThreadUnsafe();
	
	/**
	 * Determines if the engine is thread safe or better
	 * @return true if the engine is thread safe or better, false if it is thread unsafe
	 */
	public boolean isThreadSafe();
	
	/**
	 * Determines if the engine is thread isolated or better
	 * @return true if the engine is thread isolated or better, false otherwise
	 */
	public boolean isThreadIsolated();
	
	/**
	 * Determines if the engine is stateless
	 * @return true if the engine is stateless, false otherwise
	 */
	public boolean isStateless();
	
	/**
	 * Returns the scopes supported by this engine's context
	 * @return the scopes supported by this engine's context
	 */
	public String getContextScopes();
	
	/**
	 * Returns this engine's engine scoped bindings
	 * @return this engine's engine scoped bindings
	 */
	public List<BindingEntry> getEngineBindings();
	
	/**
	 * Returns this engine's global scoped bindings
	 * @return this engine's global scoped bindings
	 */
	public List<BindingEntry> getGlobalBindings();
    


}
