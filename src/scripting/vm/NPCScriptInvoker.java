package scripting.vm;

import client.MapleClient;
import server.GeneralThreadPool;
import tools.FileoutputUtil;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Eternal
 */
public class NPCScriptInvoker {

    private static final Map<MapleClient, NPCScriptVirtualMachine> virtuals = new ConcurrentHashMap<>();
    private static final ScriptEngineManager sem = new ScriptEngineManager();

    public static int runNpc(MapleClient c, int npc, int oid) {

        if (!c.canClickNPC()) {
            return -1;
        }
        if (virtuals.containsKey(c)) {
            return -1;
        }

        Invocable iv = getInvocable("npc/" + npc + ".js");
        if (iv == null) {
            return 1;
        }

        final ScriptEngine scriptengine = (ScriptEngine) iv;
        NPCScriptVirtualMachine vm = new NPCScriptVirtualMachine(c, iv, npc, oid, "run");
        scriptengine.put("self", new NPCScriptSelfFunction(vm));
        scriptengine.put("target", new NPCScriptTargetFunction(vm));

        virtuals.put(c, vm);

        c.getPlayer().setConversation(1);
        c.setClickedNPC();

        GeneralThreadPool.getInstance().execute(vm);
        return 0;
    }

    public static int runNpc(MapleClient c, int npc, int oid, String funcName) {

        if (!c.canClickNPC()) {
            return -1;
        }
        if (virtuals.containsKey(c)) {
            return -1;
        }

        Invocable iv = getInvocable("npc/" + npc + ".js");
        if (iv == null) {
            return 1;
        }

        final ScriptEngine scriptengine = (ScriptEngine) iv;
        NPCScriptVirtualMachine vm = new NPCScriptVirtualMachine(c, iv, npc, oid, funcName);
        scriptengine.put("self", new NPCScriptSelfFunction(vm));
        scriptengine.put("target", new NPCScriptTargetFunction(vm));

        virtuals.put(c, vm);

        c.getPlayer().setConversation(1);
        c.setClickedNPC();

        GeneralThreadPool.getInstance().execute(vm);
        return 0;
    }

    public static int runQuest(MapleClient c, int npc, int oid, int quest, boolean start) {

        if (!c.canClickNPC()) {
            return -1;
        }
        if (virtuals.containsKey(c)) {
            return -1;
        }

        Invocable iv = getInvocable("quest/q" + quest + ".js");
        if (iv == null) {
            return 1;
        }

        final ScriptEngine scriptengine = (ScriptEngine) iv;
        NPCScriptVirtualMachine vm = new NPCScriptVirtualMachine(c, iv, npc, oid, start ? "start" : "end");
        scriptengine.put("self", new NPCScriptSelfFunction(vm));
        scriptengine.put("target", new NPCScriptTargetFunction(vm));

        virtuals.put(c, vm);

        c.getPlayer().setConversation(1);
        c.setClickedNPC();

        GeneralThreadPool.getInstance().execute(vm);
        return 0;
    }

    public static NPCScriptVirtualMachine getVM(MapleClient c) {
        return virtuals.get(c);
    }

    public static boolean isVmConversation(MapleClient c) {
        return virtuals.containsKey(c);
    }

    public static void actionNpc(MapleClient c, int mode, int type, int selection, String text) {
        NPCScriptVirtualMachine vm = virtuals.get(c);
        if (vm != null) {
            if (type != vm.getLastMsg()) {
                dispose(c);
                return;
            }
            if ((type == 3 || type == 4 || type == 7) && selection == -1) {
                dispose(c);
                return;
            }
            if (mode == -1 || selection < -1) {
                dispose(c);
                return;
            }
            if (type == 0) {
                vm.processSay(mode);
            } else if (type == 3 || type == 5 || type == 7) {//vm type 걸어논거 바꿔줘야함
                vm.processAnswer(selection);
            } else if (type == 1 || type == 11 || type == 12) {
                vm.processAnswer(mode);
            } else if (type == 2) {
                vm.processText(text);
            }

        }
    }

    public static void dispose(MapleClient c) {
        if (c != null) {
            NPCScriptVirtualMachine vm = virtuals.remove(c);
            if (vm != null) {
                vm.forceStop();
            }
            if (c.getPlayer() != null) {
                c.getPlayer().setConversation(0);
            }
        }
    }

    public static Invocable getInvocable(String path) {
        FileReader fr = null;
        try {
            path = "scripts/vm/" + path;
            ScriptEngine engine = null;
            if (engine == null) {
                File scriptFile = new File(path);
                if (!scriptFile.exists()) {
                    return null;
                }
                engine = sem.getEngineByName("rhino");
                fr = new FileReader(scriptFile);
                engine.eval(fr);
            }
            return (Invocable) engine;
        } catch (Exception e) {
            System.err.println("Error executing vm script. Path: " + path + "\nException " + e);
            FileoutputUtil.log(FileoutputUtil.ScriptEx_Log, "Error executing vm script. Path: " + path + "\nException " + e);
            return null;
        } finally {
            try {
                if (fr != null) {
                    fr.close();
                }
            } catch (IOException ignore) {
            }
        }
    }
}
