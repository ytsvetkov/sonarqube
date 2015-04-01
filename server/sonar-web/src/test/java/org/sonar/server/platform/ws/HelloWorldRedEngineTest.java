/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.platform.ws;

import com.google.common.collect.ImmutableList;
import org.jruby.Ruby;
import org.jruby.RubyNil;
import org.jruby.RubyRuntimeAdapter;
import org.jruby.embed.EmbedEvalUnit;
import org.jruby.embed.InvokeFailedException;
import org.jruby.embed.PathType;
import org.jruby.embed.ScriptingContainer;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.builtin.IRubyObject;
import org.junit.Test;

/**
 * Created by sonar on 02/04/15.
 */
public class HelloWorldRedEngineTest {
    @Test
    public void testName() throws Exception {
        System.out.println("[" + getClass().getName() + "]");
        ScriptingContainer container = new ScriptingContainer();
        container.put("message", "local variable");
        container.put("@message", "instance variable");
        container.put("$message", "global variable");
        container.put("MESSAGE", "constant");
        String script =
                "puts message\n" +
                        "puts @message\n" +
                        "puts $message\n" +
                        "puts MESSAGE";
        container.runScriptlet(script);

    }

    @Test
    public void test_call_method() throws Exception {
        ScriptingContainer container = new ScriptingContainer();
        EmbedEvalUnit evalUnit = container.parse(PathType.CLASSPATH, "org/sonar/server/platform/ws/countdown.rb");
        evaluate(container, evalUnit, 10, 11);
    }

    private void evaluate(ScriptingContainer container, EmbedEvalUnit unit, int month, int day) {
        container.put("@month", month);
        container.put("@day", day);
        Object ret = unit.run();
        System.out.println(ret);
        container.getVarMap().clear();
    }

    @Test
    public void test_call_class_method() throws Exception {
        ScriptingContainer container = new ScriptingContainer();

        String filename = "org/sonar/server/platform/ws/tree_with_ivars.rb";
        Object receiver = container.runScriptlet(PathType.CLASSPATH, filename);
        evaluteTree(container, receiver);

    }

    private void evaluteTree(ScriptingContainer container, Object receiver) {
        container.put(receiver, "@name", "cherry blossom");
        container.put(receiver, "@shape", "oval");
        container.put(receiver, "@foliage", "deciduous");
        container.put(receiver, "@color", "pink");
        container.put(receiver, "@bloomtime", "March - April");
        container.callMethod(receiver, "update", Object.class);
        System.out.println(container.callMethod(receiver, "to_s", String.class));

        container.put(receiver, "@name", "cedar");
        container.put(receiver, "@shape", "pyramidal");
        container.put(receiver, "@foliage", "evergreen");
        container.put(receiver, "@color", "nondescript");
        container.put(receiver, "@bloomtime", "April - May");
        container.callMethod(receiver, "update", Object.class);
        System.out.println(container.callMethod(receiver, "to_s", String.class));
    }

    @Test
    public void test_call_class_method_with_multiple_source_files() throws Exception {
        ScriptingContainer container = new ScriptingContainer();
        container.setLoadPaths(ImmutableList.of("/home/sebastienl/DEV/sonarqube/server/sonar-web/src/test/resources/org/sonar/server/platform/ws"));

        String filename = "org/sonar/server/platform/ws/tree.rb";
        Object receiver = container.runScriptlet(PathType.CLASSPATH, filename);
        evaluteTree(container, receiver);
    }

    @Test
    public void test_loading_rb_class_into_interface() throws Exception {
        ScriptingContainer container = new ScriptingContainer();
        container.setLoadPaths(ImmutableList.of("org/sonar/server/platform/ws"));

        // implemented by a Ruby class
        Object receiver = container.runScriptlet("require 'database_version'\nDatabaseVersion.new");
        DatabaseVersion c = container.getInstance(receiver, DatabaseVersion.class);

        String currentVersion = c.currentVersion();
        String targetVersion = c.targetVersion();
        c.upgradeAndStart();
    }

    @Test
    public void test_calling_self_method_from_java() throws Exception {
        ScriptingContainer container = new ScriptingContainer();
        container.setLoadPaths(ImmutableList.of("org/sonar/server/platform/ws"));

        Object receiver = container.runScriptlet("require 'database_version'\ndef\ncall_upgrade_and_start\nDatabaseVersion.load_java_web_services\nend");
        DBMigrationTrigger migrationTrigger = container.getInstance(receiver, DBMigrationTrigger.class);
        migrationTrigger.callUpgradeAndStart();
    }

    @Test
    public void test_calling_self_method_from_java_directly_with_runtime() throws Exception {
        ScriptingContainer container = new ScriptingContainer();
        container.setLoadPaths(ImmutableList.of("org/sonar/server/platform/ws"));

        Ruby runtime = container.getProvider().getRuntime();

        RubyRuntimeAdapter adapter = JavaEmbedUtils.newRuntimeAdapter();
        JavaEmbedUtils.EvalUnit evalUnit = adapter.parse(runtime, "require 'database_version'\ndef\ncall_upgrade_and_start\nDatabaseVersion.load_java_web_services\nend", "call_upgrade_and_start.rb", 0);
        IRubyObject rubyObject = evalUnit.run();
        Object receiver = JavaEmbedUtils.rubyToJava(rubyObject);
        DBMigrationTrigger migrationTrigger = getInstance(runtime, receiver, DBMigrationTrigger.class);
        migrationTrigger.callUpgradeAndStart();
    }

    public <T> T getInstance(Ruby runtime, Object receiver, Class<T> clazz) {
        if (clazz == null || !clazz.isInterface()) {
            return null;
        }
        Object o;
        if (receiver == null || receiver instanceof RubyNil) {
            o = JavaEmbedUtils.rubyToJava(runtime, runtime.getTopSelf(), clazz);
        } else if (receiver instanceof IRubyObject) {
            o = JavaEmbedUtils.rubyToJava(runtime, (IRubyObject) receiver, clazz);
        } else {
            IRubyObject rubyReceiver = JavaUtil.convertJavaToRuby(runtime, receiver);
            o = JavaEmbedUtils.rubyToJava(runtime, rubyReceiver, clazz);
        }
        String name = clazz.getName();
        try {
            Class<T> c = (Class<T>) Class.forName(name, true, o.getClass().getClassLoader());
            return c.cast(o);
        } catch (ClassNotFoundException e) {
            throw new InvokeFailedException(e);
        }
    }
}
