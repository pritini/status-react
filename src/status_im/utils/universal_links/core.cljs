(ns status-im.utils.universal-links.core
  (:require [clojure.string :as string]
            [goog.string :as gstring]
            [re-frame.core :as re-frame]
            [status-im.multiaccounts.model :as multiaccounts.model]
            [status-im.chat.models :as chat]
            [status-im.constants :as constants]
            [status-im.router.core :as router]
            [status-im.utils.security :as security]
            [status-im.ui.components.react :as react]
            [status-im.ui.screens.add-new.new-chat.db :as new-chat.db]
            [status-im.navigation :as navigation]
            [status-im.utils.fx :as fx]
            [taoensso.timbre :as log]
            [status-im.acquisition.core :as acquisition]
            [status-im.wallet.choose-recipient.core :as choose-recipient]))

;; TODO(yenda) investigate why `handle-universal-link` event is
;; dispatched 7 times for the same link

;; domains should be without the trailing slash
(def domains {:external "https://join.status.im"
              :internal "status-im:/"})

(def links {:public-chat "%s/%s"
            :private-chat "%s/p/%s"
            :user        "%s/u/%s"
            :browse      "%s/b/%s"})

(defn generate-link [link-type domain-type param]
  (gstring/format (get links link-type)
                  (get domains domain-type)
                  param))

(defn universal-link? [url]
  (boolean
   (re-matches constants/regx-universal-link url)))

(defn deep-link? [url]
  (boolean
   (re-matches constants/regx-deep-link url)))

(fx/defn handle-browse [cofx {:keys [domain]}]
  (log/info "universal-links: handling browse" domain)
  {:browser/show-browser-selection domain})

(fx/defn handle-private-chat [cofx {:keys [chat-id]}]
  (log/info "universal-links: handling private chat" chat-id)
  (chat/start-chat cofx chat-id))

(fx/defn handle-public-chat [cofx {:keys [topic]}]
  (log/info "universal-links: handling public chat" topic)
  (chat/start-public-chat cofx topic {}))

(fx/defn handle-view-profile
  [{:keys [db] :as cofx} {:keys [public-key]}]
  (log/info "universal-links: handling view profile" public-key)
  (cond
    (and public-key (new-chat.db/own-public-key? db public-key))
    (navigation/navigate-to-cofx cofx :my-profile nil)

    public-key
    (navigation/navigate-to-cofx (assoc-in cofx [:db :contacts/identity] public-key) :profile nil)))

(fx/defn handle-eip681 [cofx data]
  (fx/merge cofx
            (choose-recipient/parse-eip681-uri-and-resolve-ens data)
            (navigation/navigate-to-cofx :wallet nil)))

(fx/defn handle-referrer-url [_ {:keus [referrer]}]
  ;; TODO: Use only for testing
  {::acquisition/check-referrer referrer})

(defn handle-not-found [full-url]
  (log/info "universal-links: no handler for " full-url))

(defn dispatch-url
  "Dispatch url so we can get access to re-frame/db"
  [url]
  (if-not (nil? url)
    (re-frame/dispatch [:handle-universal-link url])
    (log/debug "universal-links: no url")))

(fx/defn on-handle
  {:events [::match-value]}
  [cofx url {:keys [type] :as data}]
  (case type
    :public-chat  (handle-public-chat cofx data)
    :private-chat (handle-private-chat cofx data)
    :contact      (handle-view-profile cofx data)
    :browser      (handle-browse cofx data)
    :eip681       (handle-eip681 cofx data)
    :referrals    (hanle-referrer-url cofx data)
    (handle-not-found url)))

(fx/defn route-url
  "Match a url against a list of routes and handle accordingly"
  [cofx url]
  {::router/handle-uri {:uri url
                        :cb #(re-frame/dispatch [::match-value url %])}})

(fx/defn store-url-for-later
  "Store the url in the db to be processed on login"
  [{:keys [db]} url]
  (log/info :store-url-for-later)
  {:db (assoc db :universal-links/url url)})

(fx/defn handle-url
  "Store url in the database if the user is not logged in, to be processed
  on login, otherwise just handle it"
  [cofx url]
  (if (multiaccounts.model/logged-in? cofx)
    (route-url cofx url)
    (store-url-for-later cofx url)))

(fx/defn process-stored-event
  "Return an event description for processing a url if in the database"
  [{:keys [db] :as cofx}]
  (when-let [url (:universal-links/url db)]
    (fx/merge cofx
              {:db (dissoc db :universal-links/url)}
              (handle-url url))))

(defn unwrap-js-url [e]
  (-> e
      (js->clj :keywordize-keys true)
      :url))

(def url-event-listener
  (comp dispatch-url unwrap-js-url))

(defn initialize
  "Add an event listener for handling background->foreground transition
  and handles incoming url if the app has been started by clicking on a link"
  []
  (log/debug "universal-links: initializing")
  ;;NOTE: https://github.com/facebook/react-native/issues/15961
  ;; workaround for getInitialURL returning null when opening the
  ;; app from a universal link after closing it with the back button
  (js/setTimeout #(-> (.getInitialURL ^js react/linking)
                      (.then dispatch-url))
                 200)
  (.addEventListener ^js react/linking "url" url-event-listener))

(defn finalize
  "Remove event listener for url"
  []
  (log/debug "universal-links: finalizing")
  (.removeEventListener ^js react/linking "url" url-event-listener))
