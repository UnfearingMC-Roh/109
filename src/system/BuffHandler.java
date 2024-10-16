/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package system;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author jch50
 */
public class BuffHandler {
    private static final BuffHandler instance = new BuffHandler();
    private Map<Integer, Map<Integer, Long>> buffs = new HashMap<>();
    
    public static BuffHandler get() {
        return instance;
    }
    
    public void addBuff(int userID, int skillID, long time) {
        if (isContains(userID)) {
            Map<Integer, Long> getBuffs = getBuffs(userID);
            if (getBuffs == null) getBuffs = new HashMap<>();
            
            getBuffs.put(skillID, time);
            this.buffs.put(userID, getBuffs);
        } else {
            this.buffs.put(userID, new HashMap<>(skillID, time));
        }
    }
    
    public boolean isContains(int userID) {
        return buffs.containsKey(userID);
    }
    
    public Map<Integer, Long> getBuffs(int userID) {
        return this.buffs.get(userID);
    }
    
    public void removeBuff(int userID, int skillID) {
        if (isContains(userID)) {
            Map<Integer, Long> getBuffs = getBuffs(userID);
            if (getBuffs != null) {
                if (getBuffs.containsKey(skillID))
                    getBuffs.remove(skillID);
                this.buffs.put(userID, getBuffs);
            }
        }
    }
}
