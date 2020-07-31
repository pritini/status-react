(ns status-im.notifications.core
  (:require [re-frame.core :as re-frame]
            [taoensso.timbre :as log]
            [status-im.multiaccounts.update.core :as multiaccounts.update]
            [status-im.native-module.core :as status]
            ["react-native-push-notification" :as rn-pn]
            [quo.platform :as platform]
            [status-im.utils.fx :as fx]
            [status-im.ethereum.json-rpc :as json-rpc]
            [status-im.waku.core :as waku]))

(defn enable-ios-notifications []
  (.configure
   ^js rn-pn
   #js {:onRegister          (fn [token-data]
                               (let [token (.-token ^js token-data)]
                                 (re-frame/dispatch [::registered-for-push-notifications token])
                                 (println "TOKEN " token)))
        :onRegistrationError (fn [error]
                               (log/error "[push-notifications]" error)
                               (re-frame/dispatch [::switch-error true error]))}))

(defn disable-ios-notifications []
  (.abandonPermissions ^js rn-pn)
  (re-frame/dispatch [::unregistered-from-push-notifications]))

(re-frame/reg-fx
 ::enable
 (fn [_]
   (if platform/android?
     (status/enable-notifications)
     (enable-ios-notifications))))

(re-frame/reg-fx
 ::disable
 (fn [_]
   (if platform/android?
     (status/disable-notifications)
     (disable-ios-notifications))))

(fx/defn handle-enable-notifications-event
  {:events [::registered-for-push-notifications]}
  [cofx token]
  {::json-rpc/call [{:method     (json-rpc/call-ext-method (waku/enabled? cofx) "registerForPushNotifications")
                     :params     [token]
                     :on-success #(log/info "[push-notifications] register-success" %)
                     :on-error   #(re-frame/dispatch [::switch-error true %])}]})

(fx/defn handle-disable-notifications-event
  {:events [::unregistered-from-push-notifications]}
  [cofx]
  {::json-rpc/call [{:method     (json-rpc/call-ext-method (waku/enabled? cofx) "unregisterForPushNotifications")
                     :params     []
                     :on-success #(log/info "[push-notifications] unregister-success" %)
                     :on-error   #(re-frame/dispatch [::switch-error false %])}]})

(fx/defn notification-switch-error
  {:events [::switch-error]}
  [cofx enabled?]
  (multiaccounts.update/optimistic cofx :remote-push-notifications-enabled? (not (boolean enabled?))))

(fx/defn notification-switch
  {:events [::switch]}
  [{:keys [db] :as cofx} enabled?]
  (fx/merge cofx
            (if enabled?
              {::enable nil}
              {::disable nil})
            (multiaccounts.update/optimistic :remote-push-notifications-enabled? (boolean enabled?))))

(fx/defn notification-non-contacts-error
  {:events [::non-contacts-update-error]}
  [cofx enabled?]
  (multiaccounts.update/optimistic cofx :push-notifications-from-contacts-only? (not (boolean enabled?))))

(fx/defn notification-non-contacts
  {:events [::switch-non-contacts]}
  [{:keys [db] :as cofx} enabled?]
  (let [method (if enabled?
                 "disablePushNotificationsFromContactsOnly"
                 "enablePushNotificationsFromContactsOnly")]
    (fx/merge cofx
              {::json-rpc/call [{:method     (json-rpc/call-ext-method (waku/enabled? cofx) method)
                                 :params     []
                                 :on-success #(log/info "[push-notifications] contacts-notification-success" %)
                                 :on-error   #(re-frame/dispatch [::non-contacts-update-error enabled? %])}]}

              (multiaccounts.update/optimistic :push-notifications-from-contacts-only? (boolean enabled?)))))

(fx/defn switch-push-notifications-server-enabled
  {:events [::switch-push-notifications-server-enabled]}
  [{:keys [db] :as cofx} enabled?]
  (let [method (if enabled?
                 "startPushNotificationsServer"
                 "stopPushNotificationsServer")]
    (fx/merge cofx
              {::json-rpc/call [{:method     (json-rpc/call-ext-method (waku/enabled? cofx) method)
                                 :params     []
                                 :on-success #(log/info "[push-notifications] switch-server-enabled successful" %)
                                 :on-error   #(re-frame/dispatch [::push-notifications-server-update-error enabled? %])}]}

              (multiaccounts.update/optimistic :push-notifications-server-enabled? (boolean enabled?)))))

(fx/defn switch-send-notifications
  {:events [::switch-send-push-notifications]}
  [{:keys [db] :as cofx} enabled?]
  (let [method (if enabled?
                 "enableSendingNotifications"
                 "disableSendingNotifications")]
    (fx/merge cofx
              {::json-rpc/call [{:method     (json-rpc/call-ext-method (waku/enabled? cofx) method)
                                 :params     []
                                 :on-success #(log/info "[push-notifications] switch-send-notifications successful" %)
                                 :on-error   #(re-frame/dispatch [::push-notifications-send-update-error enabled? %])}]}

              (multiaccounts.update/optimistic :send-push-notifications? (boolean enabled?)))))

(fx/defn add-server
  {:events [::add-server]}
  [{:keys [db] :as cofx} public-key]
  (fx/merge cofx
            {::json-rpc/call [{:method     (json-rpc/call-ext-method (waku/enabled? cofx) "addPushNotificationsServer")
                               :params     [public-key]
                               :on-success #(log/info "[push-notifications] switch-send-notifications successful" %)
                               :on-error   #(re-frame/dispatch [::push-notifications-send-update-error public-key %])}]}))

