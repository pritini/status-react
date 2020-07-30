(ns status-im.router.core
  (:require [re-frame.core :as re-frame]
            [clojure.string :as string]
            [status-im.i18n :as i18n]
            [bidi.bidi :as bidi]
            [status-im.ui.screens.add-new.new-public-chat.db :as public-chat.db]
            [status-im.utils.security :as security]
            [status-im.ethereum.eip681 :as eip681]
            [status-im.ethereum.ens :as ens]
            [status-im.ethereum.core :as ethereum]
            [status-im.ethereum.resolver :as resolver]
            [status-im.ethereum.stateofus :as stateofus]
            [cljs.spec.alpha :as spec]
            [status-im.utils.fx :as fx]))

(def ethereum-scheme "ethereum:")

(def uri-schemes ["status-im://" "status-im:"])

(def web-prefixes ["https://" "http://" "https://www." "http://wwww."])

(def web2-domain "join.status.im")

(def web-urls (map #(str % web2-domain "/") web-prefixes))

(def handled-schemes (set (into uri-schemes web-urls)))

(def routes ["" {handled-schemes {["" :chat-id]       :public-chat
                                  "chat"              {["/public/" :chat-id] :public-chat}
                                  ["b/" :domain]      :browse
                                  ["browse/" :domain] :browse
                                  ["p/" :chat-id]     :private-chat
                                  ["u/" :user-id]     :user
                                  ["user/" :user-id]  :user}
                 ethereum-scheme {["" :path] :ethereum}}])

(defn match-uri [uri]
  (assoc (bidi/match-route routes uri) :uri uri))

(defn- ens-name-parse [contact-identity]
  (when (string? contact-identity)
    (string/lower-case
     (if (ens/is-valid-eth-name? contact-identity)
       contact-identity
       (stateofus/subdomain contact-identity)))))

(defn resolve-public-key
  [{:keys [chain contact-identity cb]}]
  (let [registry (get ens/ens-registries chain)
        ens-name (ens-name-parse contact-identity)]
    (resolver/pubkey registry ens-name cb)))

(defn match-contact
  [{:keys [user-id]} callback]
  (let [public-key? (and (string? user-id)
                         (string/starts-with? user-id "0x"))
        valid-key   (and (spec/valid? :global/public-key user-id)
                         (not= user-id ens/default-key))]
    (cond
      (and public-key? valid-key)
      (callback {:type       :contact
                 :public-key user-id})

      (and (not public-key?) (string? user-id))
      (let [chain      (ethereum/chain-keyword {:db nil}) ;FIXME:
            registry   (get ens/ens-registries chain)
            ens-name   (ens-name-parse user-id)
            on-success #(match-contact {:user-id %} callback)]
        (resolver/pubkey registry ens-name on-success))

      :else
      (callback {:type  :contact
                 :error :not-found}))))

(defn match-public-chat [{:keys [chat-id]} cb]
  (if (public-chat.db/valid-topic? chat-id)
    (cb {:type  :public-chat
         :topic chat-id})
    (cb {:type  :public-chat
         :error :invalid-topic})))

(defn match-private-chat [{:keys [chat-id]} cb]
  (let [valid (or (spec/valid? :global/public-key chat-id)
                  (not= chat-id ens/default-key))]
    (if valid
      (cb {:type    :private-chat
           :chat-id chat-id})
      (cb {:type  :private-chat
           :error :invalid-chat-id}))))


(defn match-browser [{:keys [domain]} cb]
  (if (security/safe-link? domain)
    (cb {:type :browser
         :url  domain})
    (cb {:type  :browser
         :error :unsafe-link})))

;; NOTE(Ferossgp): Better to handle eip681 also with router instead of regexp.
(defn match-eip681 [uri cb]
  (if-let [message (eip681/parse-uri uri)]
    (let [{:keys [paths ens-names]}
          (reduce (fn [acc path]
                    (let [address (get-in message path)]
                      (if (ens/is-valid-eth-name? address)
                        (-> acc
                            (update :paths conj path)
                            (update :ens-names conj address))
                        acc)))
                  {:paths [] :ens-names []}
                  [[:address] [:function-arguments :address]])]
      (if (empty? ens-names)
        ;; if there are no ens-names, we dispatch request-uri-parsed immediately
        (cb {:type    :eip681
             :message message
             :uri     uri})
        (cb {:type      :eip681
             :message   message
             :paths     paths
             :ens-names ens-names})))
    (cb {:type  :eip681
         :error :cannot-parse})))

(defn handle-uri [uri cb]
  (let [{:keys [handler route-params]} (match-uri uri)]
    (case handler
      :public-chat  (match-public-chat route-params cb)
      :private-chat (match-private-chat route-params cb)
      :browse       (match-browser route-params cb)
      :user         (match-contact route-params cb)
      :ethereum     (match-eip681 uri cb)
      (cb {:type :undefined
           :data uri}))))

(re-frame/reg-fx
 ::handle-uri
 (fn [{:keys [uri cb]}]
   (handle-uri uri cb)))
