(ns frontier.ui.kanji
  (:require [seesaw.core :as ui]
            [frontier.ui :refer :all]))

(def data-screens
  {:navigation "図"
   :inventory "貨"
   :databank "帳"
   :ship "船"
   :character "自"
   :skills "技"
   :commands "令"
   :mail "郵"
   :options "淘"
   :menu "出"})

(def kanji
  {:success "功"
   :freight "貨"
   :failure "敗"
   :destroy "剿"
   :mail "郵"
   :quantity "量"
   :house "廈"
   :abolish "廃"
   :warehouse "庫"
   :degrees "度"
   :join "遭"
   :store "店"
   :threat "剽"
   :domesticate "飼"
   :bounty "酬"
   :fast "早"
   :barbarian "蛮"
   :dragon "蛟"
   :income "収"
   :friend "友"
   :task "務"
   :crime "辜"
   :police "屯"
   :flourishing "旺"
   :guild "閥"
   :tower "閣"
   :castle "城"
   :fee "料"
   :class "類"
   :rescue "救"
   :topic "題"
   :grain "顆"
   :enemy "讐"
   :base "塁"
   :wall "堵"
   :border "境"
   :government "政"
   :time "頃"
   :maneuver "操"
   :compiling "撰"
   :select "択"
   :law "掟"
   :tax "掛"
   :expel "排"
   :steal "掏"
   :plunder "奪"
   :party "隊"
   :land "陸"
   :search "捜"
   :defend "防"
   :guard "守"
   :study "学"
   :war "闘"
   :colony "閻"
   :duel "戦"
   :license "允"
   :loan "債"
   :equip "備"
   :price "価"
   :warship "艦"
   :heart "心"
   :penetrate "徹"
   :think "憶"
   :constitution "憲"
   :key "鍵"
   :skill "技"
   :money "銭"
   :travel "往"
   :promote "進"
   :admiral "将"
   :subjugate "征"
   :campaign "役"})

(def categories
  {:food "食"})

(def commands
  {:attack "攻"
   :heal "療"
   :assist "扶"
   :strike "敲"
   :resist "堪"
   :shoot "射"
   :evade "避"
   :intercept "遮"
   :flee "退"
   :chase "追"
   :kill "戮"
   :retreat "斥"
   :mow-down "薙"
   :abandon "諦"
   :examine "閲"
   :modify "改"
   :teach "教"
   :instruct "授"
   :release "放"
   :request "頼"
   :hold "攬"
   :deposit "預"
   :accept "応"
   :reject "擯"
   :aim "擬"
   :stretch-bow "彎"
   :loot "摸"
   :damage "損"
   :arrest "掴"
   :follow "随"
   :help "幇"
   :log "帳"
   :technique "術"
   :rule "戒"
   :lock "錠"
   :master "師"
   :commander "帥"
   :sovereign "帝"
   :market "市"
   :beg "希"
   :sail "帆"
   :capital "都"
   :book "巻"
   :trade "貿"
   :lend "貸"
   :buy "買"
   :pierce "貫"
   :marketing "販"
   :assets "財"
   :district "郡"
   :patrol "巡"
   :repay "返"
   :province "州"})

(def crafting
  {:grind "摩"
   :print "迹"
   :craft "工"
   :melt "鎔"
   :create "造"
   :carve "彫"
   :paint "彩"
   :chain "鎖"
   :forge "鍛"
   :build "建"})

(def tools
  {:scissors "鋏"
   })

(def agriculture
  {:plant "播"
   :wheat "嘛"})

(def combat
  {:victory "捷"
   :wound "瘡"})

(def actions
  {:remove "撤"
   :delete "捨"
   :cancel "除"
   :forgive "恕"})

(def states
  {:hungry "飢"
   :alert "敏"
   :diseased "患"
   :poisoned "酔"})

(def character
  {:equip "携"
   })

(def negotiation
  {:demand "需"
   :propose "提"})

(def genders
  {:male "雄"
   :female "雌"})

(def business
  {:hire "雇"
   :cost "賃"})

(def factions
  {:criminal "賊"})

(def mail
  {
   :send "捺"})

(def attributes
  {:acidity "酸"
   :hardness "堅"
   :flavor "味"
   :agility "迅"
   :durability "逞"
   :coldness "凛"
   :heat "熱"
   })

(def weapon-attributes
  {:sharpness "鋭"})

(def armor-attributes
  {:strength "強"})

(def mining
  {:extract "抜"
   :sample "輯"
   :salt "塩"
   :iron "鉄"
   :gold "金"
   :lead "鉛"
   :ore "鉱"
   :copper "銅"
   :tin "錫"
   :steel "鋼"
   :saw "鋸"
   :silver "銀"
   :sheet-metal "鈑"
   :casting "鋳"})

(def weapons
  {:halberd "戛"
   :bomb "爆"
   :hook "鈎"
   :sword "刀"
   :bow "弩"
   :gun "銃"
   :harpoon "銛"
   :scythe "鎌"
   :dagger "鋒"
   :bullet "弾"})

(def personality-traits
  {:kind "懇"
   :intuition "勘"
   :courage "勇"
   :loyal "忠"
   :lucky "幸"
   :calm "憺"
   :intelligence "賢"
   :compassionate "憐"
   :melancholy "憂"
   :greedy "慾"
   :happy "慶"
   :wise "慧"
   :humble "慎"
   :selfish "恣"
   :angry "怒"
   })

(def mental-state
  {:fearful "慄"
   :surprised "愕"
   :happy "愉"
   :sad "愴"
   :distressed "愁"
   :confused "錯"
   :agony "悶"
   :ecstasy "悦"
   :anxious "悄"
   :virtuous "徳"})

(def biology
  {:specimen "鑑"
   :crab "蟹"
   :mantis "蟷"
   :ant "蟻"
   :toad "蟇"
   :firefly "螢"
   :snail "蝸"
   :butterfly "蝶"
   :shrimp "蝦"
   :bat "蝠"
   :hedgehog "蝟"
   :scorpion "蝎"
   })

(def reputation
  {:dishonor "恥"})

(def statuses
  {:busy "忙"
   })

(def astronomical
  {:comet "彗"
   :planet "界"})

(def speeds
  {:fast "速"})

(def movement
  {:stop "逗"})

(def navigation
  {:route "途"
   :navigate "航"})

(def tactics
  {:surround "囲"})

(defn icon
  [s tip]
  (button :text s
          :font (:osaka fonts)
          :tip tip))
