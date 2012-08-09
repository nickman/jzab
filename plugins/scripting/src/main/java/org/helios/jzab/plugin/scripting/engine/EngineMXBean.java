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



/**
 * <p>Title: EngineMXBean</p>
 * <p>Description: JMX interface for {@link Engine}, a light wrapper around {@link javax.script.ScriptEngineFactory}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>javax.script.EngineMXBean</code></p>
 */
public interface EngineMXBean {
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
     * Determines if scripts can be compiled by this engine
     * @return true if scripts can be compiled by this engine, false otherwise
     */
    public boolean isCompilable();
    
    /**
     * Determines if this engine supports the invocation of procedures in scripts that have previously been executed. 
     * @return true if the engine is invocable, false otherwise
     */
    public boolean isInvocable();


}
