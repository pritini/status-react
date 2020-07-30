(ns status-im.qr-scanner.core
  (:require [re-frame.core :as re-frame]
            [status-im.i18n :as i18n]
            [status-im.chat.models :as chat]
            [status-im.router.core :as router]
            [status-im.navigation :as navigation]
            [status-im.utils.utils :as utils]
            [status-im.wallet.choose-recipient.core :as choose-recipient]
            [status-im.ui.screens.add-new.new-chat.db :as new-chat.db]
            [status-im.utils.fx :as fx]))

(fx/defn scan-qr-code
  {:events [::scan-code]}
  [_ opts]
  {:request-permissions-fx
   {:permissions [:camera]
    :on-allowed  #(re-frame/dispatch [:navigate-to :qr-scanner opts])
    :on-denied   (fn []
                   (utils/set-timeout
                     #(utils/show-popup (i18n/label :t/error)
                                        (i18n/label :t/camera-access-error))
                     50))}})

(fx/defn set-qr-code
  {:events [:qr-scanner.callback/scan-qr-code-success]}
  [{:keys [db]} opts data]
  (when-let [handler (:handler opts)]
    {:dispatch [handler data opts]}))

(fx/defn set-qr-code-cancel
  {:events [:qr-scanner.callback/scan-qr-code-cancel]}
  [cofx opts]
  (fx/merge cofx
            (navigation/navigate-back)
            (when-let [handler (:cancel-handler opts)]
              (fn [] {:dispatch [handler opts]}))))


(fx/defn handle-browse [cofx {:keys [domain]}]
  (fx/merge cofx
            {:browser/show-browser-selection domain}
            (navigation/navigate-back)))

(fx/defn handle-private-chat [cofx {:keys [chat-id]}]
  (chat/start-chat cofx chat-id {}))

(fx/defn handle-public-chat [cofx {:keys [topic]}]
  (chat/start-public-chat cofx topic {}))

(fx/defn handle-view-profile
  [{:keys [db] :as cofx} {:keys [public-key]}]
  (cond
    (and public-key (new-chat.db/own-public-key? db public-key))
    (navigation/navigate-to-cofx cofx :tabs {:screen :my-profile})

    public-key
    (navigation/navigate-to-cofx (assoc-in cofx [:db :contacts/identity] public-key) :tabs {:screen :profile})))

(fx/defn handle-eip681 [cofx data]
  (fx/merge cofx
            (choose-recipient/parse-eip681-uri-and-resolve-ens data)
            (navigation/navigate-to-cofx :tabs {:screen :wallet} nil)))

(fx/defn match-scan
  {:events [::match-scanned-value]}
  [cofx {:keys [type] :as data}]
  (case type
    :public-chat  (handle-public-chat cofx data)
    :private-chat (handle-private-chat cofx data)
    :contact      (handle-view-profile cofx data)
    :browser      (handle-browse cofx data)
    :eip681       (handle-eip681 cofx data)
    {:utils/show-popup {:title      (i18n/label :t/unable-to-read-this-code)
                        :content    "Cannot handle this code"
                        :on-dismiss #(re-frame/dispatch [:navigate-to :home])}}))

(fx/defn on-scan
  {:events [::on-scan-success]}
  [_ uri]
  {::router/handle-uri {:uri uri
                        :cb #(re-frame/dispatch [::match-scanned-value %])}})
