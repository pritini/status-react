(ns status-im.router.core-test
  (:require [status-im.router.core :as router]
            [cljs.test :refer  [deftest are] :include-macros true]))

(def public-key "0x04fbce10971e1cd7253b98c7b7e54de3729ca57ce41a2bfb0d1c4e0a26f72c4b6913c3487fa1b4bb86125770f1743fb4459da05c1cbe31d938814cfaf36e252073")

(deftest parse-uris
  (are [uri expected] (= (router/match-uri uri) {:handler (first expected)
                                                 :route-params (second expected)
                                                 :uri uri})

    "status-im://status" [:public-chat {:chat-id "status"}]

    "status-im://u/statuse2e" [:user {:user-id "statuse2e"}]

    (str "status-im://user/" public-key) [:user {:user-id public-key}]

    "status-im://b/www.cryptokitties.co" [:browse {:domain "www.cryptokitties.co"}]

    "https://join.status.im/status" [:public-chat {:chat-id "status"}]

    "https://join.status.im/u/statuse2e" [:user {:user-id "statuse2e"}]

    (str "https://join.status.im/user/" public-key) [:user {:user-id public-key}]

    "https://join.status.im/b/www.cryptokitties.co" [:browse {:domain "www.cryptokitties.co"}]

    "ethereum:0x89205a3a3b2a69de6dbf7f01ed13b2108b2c43e7" [:ethereum {:path "0x89205a3a3b2a69de6dbf7f01ed13b2108b2c43e7"}]))
