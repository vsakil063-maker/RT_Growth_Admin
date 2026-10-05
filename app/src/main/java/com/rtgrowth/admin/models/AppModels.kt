package com.rtgrowth.admin.models

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class User(
    var name: String? = "",
    var uid: String? = "",
    var password: String? = "",
    var balance: Double? = 0.0,
    var active: Boolean? = true,
    var otp_code: String? = "",
    var referred_by: String? = null,
    var registration_date: String? = null,
    var registration_time: Long? = null,
    var verification_fee_paid: Boolean? = false,
    var wallet: WalletInfo? = null,
    var deposits: Map<String, Deposit>? = null,
    var withdrawals: Map<String, Withdrawal>? = null,
    var paragraph_jobs: Map<String, TypingTask>? = null,
    var recharges: Map<String, Recharge>? = null,
    var sendmoney: Map<String, SendMoneyItem>? = null,
    var referrals: Map<String, ReferralCommission>? = null
)

data class WalletInfo(
    var co_wallet: String? = "",
    var co_number: String? = "",
    var co_pin: String? = ""
)

data class Deposit(
    var amount: Double? = 0.0,
    var wallet: String? = "",
    var sender: String? = "",
    var txid: String? = "",
    var date: String? = "",
    var status: String? = "Pending"
)

data class Withdrawal(
    var amount: Double? = 0.0,
    var totalDeducted: Double? = 0.0,
    var wallet: String? = "",
    var date: String? = "",
    var status: String? = "Pending"
)

data class TypingTask(
    var type: String? = "",
    var lang: String? = "",
    var topic: String? = "",
    var text: String? = "",
    var amount: Double? = 0.0,
    var entry_fee: Double? = 0.0,
    var date: String? = "",
    var status: String? = "Pending",
    var admin_comment: String? = null,
    var fee_refunded: Boolean? = false
)

data class Recharge(
    var operator: String? = "",
    var type: String? = "",
    var number: String? = "",
    var amount: Double? = 0.0,
    var date: String? = "",
    var status: String? = "Pending"
)

data class SendMoneyItem(
    var id: String? = "",
    var type: String? = "",
    var sender: String? = "",
    var senderName: String? = "",
    var target: String? = "",
    var targetName: String? = "",
    var amount: Double? = 0.0,
    var fee: Double? = 0.0,
    var totalDeducted: Double? = 0.0,
    var date: String? = "",
    var status: String? = "Pending"
)

data class ReferralCommission(
    var commission: Double? = 0.0,
    var claimed: Boolean? = false,
    var unlock_timestamp: Long? = 0L,
    var date: String? = ""
)

data class GiftVoucher(
    var code: String? = "",
    var targetUid: String? = "",
    var referredUid: String? = "",
    var amount: Double? = 0.0,
    var date: String? = "",
    var status: String? = "active",
    var timestamp: Any? = null
)

data class ChatMessage(
    var sender: String? = "",
    var text: String? = "",
    var timestamp: Long? = 0L,
    var seen: Boolean? = false
)

data class GatewaySetting(
    var label: String? = "",
    var number: String? = ""
)
