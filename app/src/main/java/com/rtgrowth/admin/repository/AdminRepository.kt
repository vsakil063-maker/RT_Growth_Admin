package com.rtgrowth.admin.repository

import com.google.firebase.database.*
import com.rtgrowth.admin.models.GiftVoucher
import com.rtgrowth.admin.models.User
import java.text.SimpleDateFormat
import java.util.*

class AdminRepository {
    private val db = FirebaseDatabase.getInstance().reference

    // ১. ইউজার অ্যাক্টিভ/ইনঅ্যাক্টিভ টগল
    fun toggleUserActive(phone: String, makeActive: Boolean, onDone: () -> Unit) {
        db.child("users").child(phone).child("active").setValue(makeActive).addOnSuccessListener {
            onDone()
        }
    }

    // ২. নির্দিষ্ট ইউজারের ব্যালেন্স যোগ/বিয়োগ
    fun modifyUserBalance(phone: String, amount: Double, isAdd: Boolean, onDone: (Double) -> Unit) {
        val balanceRef = db.child("users").child(phone).child("balance")
        balanceRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                var bal = (currentData.value as? Number)?.toDouble() ?: 0.0
                bal = if (isAdd) bal + amount else (bal - amount).coerceAtLeast(0.0)
                currentData.value = bal
                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                val finalBal = (snapshot?.value as? Number)?.toDouble() ?: 0.0
                onDone(finalBal)
            }
        })
    }

    // ৩. নির্দিষ্ট ইউজার ব্যালেন্স ৳০.০০ করা
    fun resetSingleUserBalance(phone: String, onDone: () -> Unit) {
        db.child("users").child(phone).child("balance").setValue(0.00).addOnSuccessListener {
            onDone()
        }
    }

    // ৪. সকল ইউজারের ব্যালেন্স একসাথে ৳০.০০ করা (Bulk Balance Reset)
    fun resetAllUsersBalance(userPhones: List<String>, onDone: (Boolean) -> Unit) {
        val updates = mutableMapOf<String, Any>()
        userPhones.forEach { phone ->
            updates["users/$phone/balance"] = 0.00
        }
        db.updateChildren(updates).addOnCompleteListener { task ->
            onDone(task.isSuccessful)
        }
    }

    // ৫. ইউজার ডিলিট করা
    fun deleteUser(phone: String, onDone: () -> Unit) {
        db.child("users").child(phone).removeValue().addOnSuccessListener {
            onDone()
        }
    }

    // ৬. ডিপোজিট অনুমোদন
    fun approveDeposit(phone: String, depId: String, amount: Double, onDone: () -> Unit) {
        val balanceRef = db.child("users").child(phone).child("balance")
        balanceRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val bal = (currentData.value as? Number)?.toDouble() ?: 0.0
                currentData.value = bal + amount
                return Transaction.success(currentData)
            }
            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                db.child("users").child(phone).child("deposits").child(depId).child("status").setValue("Success")
                onDone()
            }
        })
    }

    // ৭. ডিপোজিট রিজেক্ট
    fun rejectDeposit(phone: String, depId: String, onDone: () -> Unit) {
        db.child("users").child(phone).child("deposits").child(depId).child("status").setValue("Rejected").addOnSuccessListener {
            onDone()
        }
    }

    // ৮. উইথড্র অনুমোদন
    fun approveWithdrawal(phone: String, wdId: String, onDone: () -> Unit) {
        db.child("users").child(phone).child("withdrawals").child(wdId).child("status").setValue("Success").addOnSuccessListener {
            onDone()
        }
    }

    // ৯. উইথড্র রিজেক্ট ও টাকা রিফান্ড
    fun rejectWithdrawal(phone: String, wdId: String, refundAmt: Double, onDone: () -> Unit) {
        val balanceRef = db.child("users").child(phone).child("balance")
        balanceRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val bal = (currentData.value as? Number)?.toDouble() ?: 0.0
                currentData.value = bal + refundAmt
                return Transaction.success(currentData)
            }
            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                db.child("users").child(phone).child("withdrawals").child(wdId).child("status").setValue("Rejected")
                onDone()
            }
        })
    }

    // ১০. টাইপিং টাস্ক অনুমোদন + নিট লাভের ৫% রেফারেল কমিশন (রাত ১২টা আনলক)
    fun approveTypingTask(
        phone: String,
        taskId: String,
        reward: Double,
        entryFee: Double,
        allUsersMap: Map<String, User>,
        onDone: (commission: Double) -> Unit
    ) {
        val userBalanceRef = db.child("users").child(phone).child("balance")
        userBalanceRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val bal = (currentData.value as? Number)?.toDouble() ?: 0.0
                currentData.value = bal + reward
                return Transaction.success(currentData)
            }
            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                db.child("users").child(phone).child("paragraph_jobs").child(taskId).child("status").setValue("Success")

                // রেফারেল কমিশন
                val user = allUsersMap[phone]
                val refUid = user?.referred_by
                var givenCommission = 0.0

                if (!refUid.isNullOrEmpty() && refUid != "None") {
                    val netProfit = (reward - entryFee).coerceAtLeast(0.0)
                    givenCommission = netProfit * 0.05 // ৫% নিট লাভ

                    for ((refPhone, potentialRef) in allUsersMap) {
                        if (potentialRef.uid == refUid) {
                            val nextMidnight = Calendar.getInstance().apply {
                                add(Calendar.DAY_OF_YEAR, 1)
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                            }.timeInMillis

                            val refPath = db.child("users").child(refPhone).child("referrals").child(phone)
                            refPath.child("commission").runTransaction(object : Transaction.Handler {
                                override fun doTransaction(currentComm: MutableData): Transaction.Result {
                                    val c = (currentComm.value as? Number)?.toDouble() ?: 0.0
                                    currentComm.value = c + givenCommission
                                    return Transaction.success(currentComm)
                                }
                                override fun onComplete(e: DatabaseError?, com: Boolean, snap: DataSnapshot?) {
                                    refPath.updateChildren(
                                        mapOf(
                                            "claimed" to false,
                                            "unlock_timestamp" to nextMidnight,
                                            "date" to SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                                        )
                                    )
                                }
                            })
                            break
                        }
                    }
                }
                onDone(givenCommission)
            }
        })
    }

    // ১১. টাইপিং টাস্ক রিজেক্ট ও ফি রিফান্ড
    fun rejectTypingTask(phone: String, taskId: String, entryFee: Double, reason: String, onDone: () -> Unit) {
        if (entryFee > 0) {
            val balanceRef = db.child("users").child(phone).child("balance")
            balanceRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val bal = (currentData.value as? Number)?.toDouble() ?: 0.0
                    currentData.value = bal + entryFee
                    return Transaction.success(currentData)
                }
                override fun onComplete(e: DatabaseError?, c: Boolean, s: DataSnapshot?) {}
            })
        }

        db.child("users").child(phone).child("paragraph_jobs").child(taskId).updateChildren(
            mapOf(
                "status" to "Rejected",
                "admin_comment" to reason,
                "fee_refunded" to (entryFee > 0)
            )
        ).addOnSuccessListener {
            onDone()
        }
    }

    // ১২. সিকিউর ২১ ডিজিট ভাউচার জেনারেটর
    fun generateSecureGiftVoucher(claimerUid: String, referredUid: String, amount: Double, onDone: (String) -> Unit) {
        val amountPadded = String.format("%05d", amount.toInt())
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val random4 = (1..4).map { chars.random() }.joinToString("")
        val finalVoucherCode = "TBS${amountPadded}${claimerUid}QU${random4}R"
        val dateStr = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date())

        val voucher = GiftVoucher(
            code = finalVoucherCode,
            targetUid = claimerUid,
            referredUid = referredUid,
            amount = amount,
            date = dateStr,
            status = "active",
            timestamp = ServerValue.TIMESTAMP
        )

        db.child("valid_gift_vouchers").child(finalVoucherCode).setValue(voucher).addOnSuccessListener {
            onDone(finalVoucherCode)
        }
    }

    // ১৩. সেন্ডমানি অনুমোদন
    fun approveSendMoney(id: String, sender: String, target: String, amount: Double, senderName: String, onDone: () -> Unit) {
        val targetBalanceRef = db.child("users").child(target).child("balance")
        targetBalanceRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val bal = (currentData.value as? Number)?.toDouble() ?: 0.0
                currentData.value = bal + amount
                return Transaction.success(currentData)
            }
            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                val dateStr = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date())
                db.child("users").child(target).child("sendmoney").child(id).setValue(
                    mapOf(
                        "id" to id, "type" to "Received", "sender" to sender,
                        "senderName" to senderName, "amount" to amount, "date" to dateStr, "status" to "Success"
                    )
                )
                db.child("pending_send_money").child(id).child("status").setValue("Success")
                db.child("users").child(sender).child("sendmoney").child(id).child("status").setValue("Success")
                onDone()
            }
        })
    }

    // ১৪. সেন্ডমানি রিজেক্ট ও রিফান্ড
    fun rejectSendMoney(id: String, sender: String, totalDeducted: Double, onDone: () -> Unit) {
        val senderBalRef = db.child("users").child(sender).child("balance")
        senderBalRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val bal = (currentData.value as? Number)?.toDouble() ?: 0.0
                currentData.value = bal + totalDeducted
                return Transaction.success(currentData)
            }
            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                db.child("pending_send_money").child(id).child("status").setValue("Rejected")
                db.child("users").child(sender).child("sendmoney").child(id).child("status").setValue("Rejected")
                onDone()
            }
        })
    }

    // ১৫. মোবাইল রিচার্জ অনুমোদন ও রিজেক্ট
    fun approveRecharge(phone: String, id: String, onDone: () -> Unit) {
        db.child("users").child(phone).child("recharges").child(id).child("status").setValue("Success").addOnSuccessListener {
            onDone()
        }
    }

    fun rejectRecharge(phone: String, id: String, amount: Double, onDone: () -> Unit) {
        val balanceRef = db.child("users").child(phone).child("balance")
        balanceRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val bal = (currentData.value as? Number)?.toDouble() ?: 0.0
                currentData.value = bal + amount
                return Transaction.success(currentData)
            }
            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                db.child("users").child(phone).child("recharges").child(id).child("status").setValue("Rejected")
                onDone()
            }
        })
    }

    // ১৬. লাইভ চ্যাট মেসেজ পাঠানো
    fun sendAdminChatMessage(phone: String, text: String, onDone: () -> Unit) {
        val newMsgRef = db.child("chats").child(phone).child("messages").push()
        val msg = mapOf(
            "sender" to "admin",
            "text" to text,
            "timestamp" to System.currentTimeMillis(),
            "seen" to true
        )
        newMsgRef.setValue(msg).addOnSuccessListener {
            onDone()
        }
    }

    // ১৭. গেটওয়ে ও সিস্টেম সেটিংস সেভ
    fun saveSystemSettings(
        gateways: Map<String, Map<String, String>>,
        links: Map<String, String>,
        otpLength: Int,
        onDone: () -> Unit
    ) {
        db.child("admin_settings/deposit_targets").setValue(gateways)
        db.child("admin_settings/links").setValue(links)
        db.child("settings/otpLength").setValue(otpLength).addOnSuccessListener {
            onDone()
        }
    }
}
