/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

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
package server;

import java.util.LinkedHashMap;
import java.util.Map;

import java.util.Map.Entry;

public class MapleAchievements {

    private Map<Integer, MapleAchievement> achievements = new LinkedHashMap<Integer, MapleAchievement>();
    private static MapleAchievements instance = new MapleAchievements();

    protected MapleAchievements() {
        //achievements.put(1, new MapleAchievement("got their first point", 1000, false));
        achievements.put(2, new MapleAchievement("레벨 30 달성!", 3000, false));//ㅇ
        achievements.put(3, new MapleAchievement("레벨 70 달성!", 7000, false));//ㅇ
        achievements.put(4, new MapleAchievement("레벨 120 달성!", 12000, false));//ㅇ
        achievements.put(5, new MapleAchievement("레벨 200 달성!", 20000, false));//ㅇ
        achievements.put(7, new MapleAchievement("당신...인기 좀 있는걸?!", 1000, false));//ㅇ
        achievements.put(8, new MapleAchievement("소싯적 헤네시스에서 좀 노셨군요?", 3000, false));//ㅇ
        achievements.put(9, new MapleAchievement("시간의 승리자", 2000, false));
        achievements.put(10, new MapleAchievement("진정한 시간의 승리자", 2000, false));
        achievements.put(11, new MapleAchievement("운영자 뒷담화 금지...-_-!", 1000, false));//ㅇ
        achievements.put(12, new MapleAchievement("야쿠자 아내 쫓아내기", 1500, false));
        achievements.put(13, new MapleAchievement("원정대 보스 클리어", 2500, false));//ㅇ
        achievements.put(14, new MapleAchievement("너의 진짜 얼굴을 보여줘!", 1500, false));
        achievements.put(15, new MapleAchievement("해외라고 별거 없네..쩝", 1000, false));//ㅇ
        achievements.put(16, new MapleAchievement("아주 큰 나무를 베어보았다", 3000, false));
        achievements.put(17, new MapleAchievement("불너구리금융 재패", 3000, false));
        achievements.put(18, new MapleAchievement("나...좀 강하네...?", 3000, false));//ㅇ
        achievements.put(19, new MapleAchievement("OX퀴즈 우승하기'", 5000, false));
        achievements.put(20, new MapleAchievement("고지를향해서 완주하기", 1000, false));
        achievements.put(21, new MapleAchievement("올라올라 우승하기", 5000, false));
        achievements.put(22, new MapleAchievement("헬모드 보스 클리어하기", 50000));
        achievements.put(23, new MapleAchievement("카오스 자쿰 처치하기", 10000, false));
        achievements.put(24, new MapleAchievement("카오스 혼테일 처치하기", 20000, false));
        //achievements.put(25, new MapleAchievement("won the event 'Survival Challenge'", 5000, false));
        achievements.put(26, new MapleAchievement("새로운 무림고수 등장!", 3000, false));
        achievements.put(27, new MapleAchievement("소주는 역시? 진로!", 3000, false));
        achievements.put(28, new MapleAchievement("데미지 10만 달성", 4000, false));
        achievements.put(29, new MapleAchievement("데미지 50만 달성", 5000, false));
        achievements.put(30, new MapleAchievement("데미지 99만 달성", 10000, false));
        achievements.put(31, new MapleAchievement("100만 메소 넘기", 1000, false));
        achievements.put(32, new MapleAchievement("1000만 메소 넘기", 2000, false));
        achievements.put(33, new MapleAchievement("1억 메소 넘기", 3000, false));
        achievements.put(34, new MapleAchievement("10억 메소 넘기", 4000, false));
        achievements.put(35, new MapleAchievement("연합을 이끌 자! 바로 나", 300, false));//ㅇ
        //achievements.put(36, new MapleAchievement("패밀리 만들기", 2500, false));
        achievements.put(37, new MapleAchievement("크림슨우드 파티 퀘스트 클리어", 4000, false));
        achievements.put(38, new MapleAchievement("반 레온 처치하기", 25000, false));
        achievements.put(39, new MapleAchievement("시그너스 처치하기", 100000, false));
        achievements.put(40, new MapleAchievement("130제 아이템 착용하기", 2000, false));
        achievements.put(41, new MapleAchievement("140제 아이템 착용하기", 3000, false));
    }

    public static MapleAchievements getInstance() {
        return instance;
    }

    public MapleAchievement getById(int id) {
        return achievements.get(id);
    }

    public Integer getByMapleAchievement(MapleAchievement ma) {
        for (Entry<Integer, MapleAchievement> achievement : this.achievements.entrySet()) {
            if (achievement.getValue() == ma) {
                return achievement.getKey();
            }
        }
        return null;
    }
}
