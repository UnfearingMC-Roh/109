/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package server;

/**
 *
 * @author jmk50
 */
public class CustomSetEffect {
    private int bossDAMr;
    private int ignoreTargetDEF;

    public CustomSetEffect(int bossDAMr, int ignoreTargetDEF) {
        this.bossDAMr = Math.max(0, Math.min(bossDAMr, 100));
        this.ignoreTargetDEF = Math.max(0, Math.min(ignoreTargetDEF, 100));
    }

    public int getBossDAMr() {
        return bossDAMr;
    }

    public int getIgnoreTargetDEF() {
        return ignoreTargetDEF;
    }
}
