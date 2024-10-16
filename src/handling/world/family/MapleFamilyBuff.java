/*
 This file is part of the ZeroFusion MapleStory Server
 Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc> 
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>
 ZeroFusion organized by "RMZero213" <RMZero213@hotmail.com>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License version 3
 as published by the Free Software Foundation. You may not use, modify
 or distribute this program under any other version of the
 GNU Affero General Public License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package handling.world.family;

import client.MapleBuffStat;
import client.MapleCharacter;
import java.util.EnumMap;
import java.util.concurrent.ScheduledFuture;
import server.MapleItemInformationProvider;
import server.MapleStatEffect;
import server.MapleStatEffect.CancelEffectAction;
import server.Timer.BuffTimer;
import tools.packet.TemporaryStatsPacket;

public enum MapleFamilyBuff {

    //※ 나만의 경험치 효과나 드롭률 이벤트와 겹칠 경우 효력이 무시된다.
    Teleport("패밀리원에게 바로 이동", "[대상] 자기 자신\r\n[효과] 원하는 패밀리원이 있는 장소로 바로 이동한다.", 0, 0, 0, 300, 190000),
    Summon("패밀리원 바로 소환", "[대상] 패밀리원 1명\r\n[효과] 원하는 패밀리원을 자신이 있는 맵으로 바로 소환한다.", 1, 0, 0, 500, 190001),
    Drop_12_15("나만의 드롭률 1.2배(15분)", "[대상] 자기 자신\r\n[지속시간] 15분\r\n[효과] 몬스터 사냥 드롭률이1.2배로 향상.\r\n※ 나만의 경험치 효과나 드롭률 이벤트와 겹칠 경우 효력이 무시된다.", 3, 15, 120, 700, 190002),
    EXP_12_15("나만의 경험치 1.2배(15분)", "[대상] 자기 자신\r\n[지속시간] 15분\r\n[효과] 몬스터 사냥 시 얻는 경험치가 1.2배로 향상.\r\n※ 나만의 드롭률 효과나 경험치 이벤트와 겹칠 경우 효력이 무시된다.", 2, 15, 120, 800, 190003),
    Drop_12_30("나만의 드롭률 1.2배(30분)", "[대상] 자기 자신\r\n[지속시간] 30분\r\n[효과] 몬스터 사냥 드롭률이 1.2배로 향상.\r\n※ 나만의 경험치 효과나 드롭률 이벤트와 겹칠 경우 효력이 무시된다.", 3, 30, 120, 1000, 190004),
    EXP_12_30("나만의 경험치 1.2배(30분)", "[대상] 자기 자신\r\n[지속시간] 30분\r\n[효과] 몬스터 사냥 시 얻는 경험치가 1.2배로 향상.\r\n※ 나만의 드롭률 효과나 경험치 이벤트와 겹칠 경우 효력이 무시된다.", 2, 30, 120, 1200, 190005),
    
    Drop_15_15("나만의 드롭률 1.5배(15분)", "[대상] 자기 자신\r\n[지속시간] 15분\r\n[효과] 몬스터 사냥 드롭률이1.5배로 향상.\r\n※ 나만의 경험치 효과나 드롭률 이벤트와 겹칠 경우 효력이 무시된다.", 6, 15, 120, 1500, 190016),
    EXP_15_15("나만의 경험치 1.5배(15분)", "[대상] 자기 자신\r\n[지속시간] 15분\r\n[효과] 몬스터 사냥 시 얻는 경험치가 1.5배로 향상.\r\n※ 나만의 드롭률 효과나 경험치 이벤트와 겹칠 경우 효력이 무시된다.", 5, 15, 120, 1700, 190017),
    Drop_15_30("나만의 드롭률 1.5배(30분)", "[대상] 자기 자신\r\n[지속시간] 30분\r\n[효과] 몬스터 사냥 드롭률이 1.5배로 향상.\r\n※ 나만의 경험치 효과나 드롭률 이벤트와 겹칠 경우 효력이 무시된다.",6, 30, 120, 1700, 190018),
    EXP_15_30("나만의 경험치 1.5배(30분)", "[대상] 자기 자신\r\n[지속시간] 30분\r\n[효과] 몬스터 사냥 시 얻는 경험치가 1.5배로 향상.\r\n※ 나만의 드롭률 효과나 경험치 이벤트와 겹칠 경우 효력이 무시된다.", 5, 30, 120, 1900, 190019),
    
    EXP_Party_12_15("내 파티 경험치 1.2배 (15분)", "[발동조건] 파티\n[지속시간] 30분\n[효과] 몬스터 사냥 시 얻는 경험치가 1.2배로 향상.\r\n ※ 나만의 경험치 효과나 경험치 이벤트와 겹칠 경우 효력이 무시된다.", 2, 15, 120, 7000, 190008),
    Drop_Party_12_15("내 파티 드롭률 1.2배 (15분)", "[발동조건] 파티\n[지속시간] 30분\n[효과] 몬스터 사냥 드롭률이 1.2배로 향상.\r\n ※ 나만의 드롭률 효과나 드롭률 이벤트와 겹칠 경우 효력이 무시된다.", 3, 15, 150, 5000, 190011),
    EXP_Party_12_30("내 파티 경험치 1.2배 (30분)", "[발동조건] 파티\n[지속시간] 30분\n[효과] 몬스터 사냥 시 얻는 경험치가 1.2배로 향상.\r\n ※ 나만의 경험치 효과나 경험치 이벤트와 겹칠 경우 효력이 무시된다.", 2, 30, 120, 10000, 190020),
    Drop_Party_12_30("내 파티 드롭률 1.2배 (30분)", "[발동조건] 파티\n[지속시간] 30분\n[효과] 몬스터 사냥 드롭률이 1.2배로 향상.\r\n ※ 나만의 드롭률 효과나 드롭률 이벤트와 겹칠 경우 효력이 무시된다.", 3, 30, 150, 8000, 190021),
    
    EXP_Party_15_15("내 파티 경험치 1.5배 (15분)", "[발동조건] 파티\n[지속시간] 15분\n[효과] 몬스터 사냥 시 얻는 경험치가 1.5배로 향상.\r\n ※ 나만의 경험치 효과나 경험치 이벤트와 겹칠 경우 효력이 무시된다.", 5, 15, 120, 12000, 190012),
    Drop_Party_15_15("내 파티 드롭률 1.5배 (15분)", "[발동조건] 파티\n[지속시간] 15분\n[효과] 몬스터 사냥 드롭률이 1.5배로 향상.\r\n ※ 나만의 드롭률 효과나 드롭률 이벤트와 겹칠 경우 효력이 무시된다.", 6, 15, 150, 10000, 190013),
    EXP_Party_15_30("내 파티 경험치 1.5배 (30분)", "[발동조건] 파티\n[지속시간] 30분\n[효과] 몬스터 사냥 시 얻는 경험치가 1.5배로 향상.\r\n ※ 나만의 경험치 효과나 경험치 이벤트와 겹칠 경우 효력이 무시된다.", 5, 30, 120, 14000, 190014),
    Drop_Party_15_30("내 파티 드롭률 1.5배 (30분)", "[발동조건] 파티\n[지속시간] 30분\n[효과] 몬스터 사냥 드롭률이 1.5배로 향상.\r\n ※ 나만의 드롭률 효과나 드롭률 이벤트와 겹칠 경우 효력이 무시된다.", 6, 30, 150, 12000, 190015),
    Bonding("패밀리원의 단결(30분)", "[발동조건] 가계도에 보이는 하위 패밀리원이 6명 이상 로그인\r\n[지속시간] 30분\r\n[효과] 드롭률과 경험치를 1.5배로 향상. ※ 나만의 드롭률, 나만의 경험치 효과나 다른 경험치, 드롭률 이벤트와 겹칠 경우 효력이 무시된다.", 4, 30, 150, 3000, 190006);
    // 0=tele, 1=summ, 2=drop, 3=exp, 4=both
    ;
    public String name, desc;
    public int rep, type, questID, duration, effect;
    public EnumMap<MapleBuffStat, Integer> effects;

    private MapleFamilyBuff(String name, String desc, int type, int duration, int effect, int rep, int questID) {
        this.name = name;
        this.desc = desc;
        this.rep = rep;
        this.type = type;
        this.questID = questID;
        this.duration = duration;
        this.effect = effect;
        setEffects();
    }

    public int getEffectId() {
//        switch (type) {
//            case 2: //exp
//                return 2450018;
//            case 3: //drop
//                return 2022694;
//        }
        return 2000000; //custom
    }

    public final void setEffects() {
        //custom
        this.effects = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
        //this.effects = new EnumMap<>(MapleBuffStat.class);
        switch (type) {
            case 2: //exp
                effects.put(MapleBuffStat.EXPRATE, effect);
                effects.put(MapleBuffStat.DROP_RATE, 100);
//                effects.remove(MapleBuffStat.DROP_RATE);
                break;
            case 3: //drop
                effects.put(MapleBuffStat.DROP_RATE, effect);
                effects.put(MapleBuffStat.EXPRATE, 100);
//                effects.remove(MapleBuffStat.EXPRATE);
                //effects.put(MapleBuffStat.MESO_RATE, effect);
                break;
            case 4: //both
                effects.put(MapleBuffStat.EXPRATE, effect);
                effects.put(MapleBuffStat.DROP_RATE, effect);
                //effects.put(MapleBuffStat.MESO_RATE, effect);
                break;
        }
    }

    public void applyTo(MapleCharacter chr) {
        MapleStatEffect eff = MapleItemInformationProvider.getInstance().getItemEffect(getEffectId());
        eff.applyTo(chr);
        final CancelEffectAction cancelAction = new CancelEffectAction(chr, eff, System.currentTimeMillis(), effects);
        final ScheduledFuture<?> schedule = BuffTimer.getInstance().schedule(cancelAction, duration * 60000);
        chr.getClient().getSession().write(TemporaryStatsPacket.giveBuff(-getEffectId(), duration * 60000, effects, null));
        chr.cancelEffect(eff, -1, effects, true);
        chr.registerEffect(eff, System.currentTimeMillis(), schedule, effects, false, duration * 60000, chr.getId());
    }
}
