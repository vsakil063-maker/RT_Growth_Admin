package com.rtgrowth.admin.ui

import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import com.rtgrowth.admin.databinding.ActivityMainBinding
import com.rtgrowth.admin.models.*
import com.rtgrowth.admin.repository.AdminRepository
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val repository = AdminRepository()
    private val db = FirebaseDatabase.getInstance().reference

    private val allUsers = mutableMapOf<String, User>()
    private val pendingSendMoneyMap = mutableMapOf<String, SendMoneyItem>()
    private val validGiftVouchers = mutableMapOf<String, GiftVoucher>()
    private val chatsMap = mutableMapOf<String, Any>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupButtons()
        startRealtimeListeners()
    }

    private fun setupButtons() {
        // ১. ইউজার ডিরেক্টরি
        binding.btnDirectory.setOnClickListener { showUserDirectoryDialog() }

        // ২. ডিপোজিটস
        binding.btnDeposits.setOnClickListener { showDepositsDialog() }

        // ৩. উইথড্রয়ালস
        binding.btnWithdrawals.setOnClickListener { showWithdrawalsDialog() }

        // ৪. সেন্ডমানি
        binding.btnSendMoney.setOnClickListener { showSendMoneyDialog() }

        // ৫. রিচার্জ
        binding.btnRecharges.setOnClickListener { showRechargesDialog() }

        // ৬. গিফট ভাউচার
        binding.btnGiftVouchers.setOnClickListener { showGiftVoucherDialog() }

        // ৭. টাইপিং টাস্ক
        binding.btnTypingTasks.setOnClickListener { showTypingTasksDialog() }

        // ৮. সাপোর্ট চ্যাট
        binding.btnSupportChat.setOnClickListener { showSupportChatDialog() }

        // ৯. ব্যালেন্স রিসেট
        binding.btnBalanceReset.setOnClickListener { showBalanceResetDialog() }

        // ১০. সিস্টেম সেটিংস
        binding.btnSettings.setOnClickListener { showSystemSettingsDialog() }
    }

    private fun startRealtimeListeners() {
        // ইউজার ও ট্রানজ্যাকশন ডাটা লাইভ সিঙ্ক
        db.child("users").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allUsers.clear()
                var totalUsersCount = 0
                var totalDepositsSum = 0.0
                var todayDepositsSum = 0.0
                var totalWithdrawalsSum = 0.0
                var todayWithdrawalsSum = 0.0
                var totalWorkDoneSum = 0.0
                var totalUserProfitsSum = 0.0
                var totalVolumeSum = 0.0

                var pendingDepCount = 0
                var pendingWdCount = 0
                var pendingRechargeCount = 0
                var pendingTypingCount = 0

                val todayDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

                for (userSnap in snapshot.children) {
                    val phone = userSnap.key ?: continue
                    val user = userSnap.getValue(User::class.java) ?: continue
                    allUsers[phone] = user

                    totalUsersCount++
                    totalVolumeSum += (user.balance ?: 0.0)

                    // Deposits
                    user.deposits?.values?.forEach { dep ->
                        val amt = dep.amount ?: 0.0
                        if (dep.status == "Pending") pendingDepCount++
                        else if (dep.status == "Success") {
                            totalDepositsSum += amt
                            if (dep.date?.contains(todayDate) == true) todayDepositsSum += amt
                        }
                    }

                    // Withdrawals
                    user.withdrawals?.values?.forEach { wd ->
                        val amt = wd.amount ?: 0.0
                        if (wd.status == "Pending") pendingWdCount++
                        else if (wd.status == "Success") {
                            totalWithdrawalsSum += amt
                            if (wd.date?.contains(todayDate) == true) todayWithdrawalsSum += amt
                        }
                    }

                    // Typing Jobs
                    user.paragraph_jobs?.values?.forEach { job ->
                        val amt = job.amount ?: 0.0
                        if (job.status == "Pending") pendingTypingCount++
                        else if (job.status == "Success") {
                            totalUserProfitsSum += amt
                            if (job.date?.contains(todayDate) == true) totalWorkDoneSum += amt
                        }
                    }

                    // Recharges
                    user.recharges?.values?.forEach { rc ->
                        if (rc.status == "Pending") pendingRechargeCount++
                    }
                }

                // UI আপডেট
                binding.tvTotalUsers.text = totalUsersCount.toString()
                binding.tvTotalDeposits.text = "৳${String.format("%.2f", totalDepositsSum)}"
                binding.tvTodayDeposits.text = "Today: ৳${String.format("%.2f", todayDepositsSum)}"
                binding.tvTotalWithdrawals.text = "৳${String.format("%.2f", totalWithdrawalsSum)}"
                binding.tvTodayWithdrawals.text = "Today: ৳${String.format("%.2f", todayWithdrawalsSum)}"
                binding.tvTotalWorkDone.text = "৳${String.format("%.2f", totalWorkDoneSum)}"
                binding.tvUserProfits.text = "৳${String.format("%.2f", totalUserProfitsSum)}"
                binding.tvTotalVolume.text = "৳${String.format("%.2f", totalVolumeSum)}"

                binding.btnDeposits.text = "💰 Deposits ($pendingDepCount)"
                binding.btnWithdrawals.text = "💸 Withdrawals ($pendingWdCount)"
                binding.btnRecharges.text = "📱 Recharges ($pendingRechargeCount)"
                binding.btnTypingTasks.text = "📝 Typing Tasks ($pendingTypingCount)"
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        // সেন্ডমানি রিকোয়েস্ট সিঙ্ক
        db.child("pending_send_money").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                pendingSendMoneyMap.clear()
                var pendingCount = 0
                for (itemSnap in snapshot.children) {
                    val item = itemSnap.getValue(SendMoneyItem::class.java) ?: continue
                    val id = itemSnap.key ?: continue
                    pendingSendMoneyMap[id] = item
                    if (item.status == "Pending") pendingCount++
                }
                binding.btnSendMoney.text = "🔄 Send Money ($pendingCount)"
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // গিফট ভাউচার সিঙ্ক
        db.child("valid_gift_vouchers").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                validGiftVouchers.clear()
                for (snap in snapshot.children) {
                    val v = snap.getValue(GiftVoucher::class.java) ?: continue
                    val code = snap.key ?: continue
                    validGiftVouchers[code] = v
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // ==========================================
    // ১. ইউজার ডিরেক্টরি ডায়ালগ
    // ==========================================
    private fun showUserDirectoryDialog() {
        val userList = allUsers.entries.map { (phone, user) ->
            "${user.name ?: "User"} | $phone | UID: ${user.uid}\nBalance: ৳${user.balance} | Active: ${user.active}"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("User Directory (${allUsers.size} Users)")
            .setItems(userList) { _, which ->
                val phone = allUsers.keys.toList()[which]
                showUserActionOptions(phone)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showUserActionOptions(phone: String) {
        val user = allUsers[phone] ?: return
        val options = arrayOf(
            if (user.active == true) "Deactivate Account" else "Activate Account",
            "Add Balance (+)",
            "Subtract Balance (-)",
            "Reset Balance (৳0.00)",
            "Delete Profile"
        )

        AlertDialog.Builder(this)
            .setTitle("Manage: ${user.name} ($phone)")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> repository.toggleUserActive(phone, !(user.active ?: true)) {
                        Toast.makeText(this, "Status updated!", Toast.LENGTH_SHORT).show()
                    }
                    1 -> promptModifyBalance(phone, true)
                    2 -> promptModifyBalance(phone, false)
                    3 -> repository.resetSingleUserBalance(phone) {
                        Toast.makeText(this, "Balance reset to ৳0.00!", Toast.LENGTH_SHORT).show()
                    }
                    4 -> repository.deleteUser(phone) {
                        Toast.makeText(this, "Account deleted!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun promptModifyBalance(phone: String, isAdd: Boolean) {
        val input = EditText(this).apply { hint = "Enter amount (৳)" }
        AlertDialog.Builder(this)
            .setTitle(if (isAdd) "Add Balance" else "Subtract Balance")
            .setView(input)
            .setPositiveButton("Submit") { _, _ ->
                val amt = input.text.toString().toDoubleOrNull()
                if (amt != null && amt > 0) {
                    repository.modifyUserBalance(phone, amt, isAdd) { newBal ->
                        Toast.makeText(this, "Updated! New Balance: ৳$newBal", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ==========================================
    // ২. ডিপোজিট অনুমোদন ডায়ালগ
    // ==========================================
    private fun showDepositsDialog() {
        val pendingDeposits = mutableListOf<Triple<String, String, Deposit>>()
        allUsers.forEach { (phone, user) ->
            user.deposits?.forEach { (id, dep) ->
                if (dep.status == "Pending") pendingDeposits.add(Triple(phone, id, dep))
            }
        }

        if (pendingDeposits.isEmpty()) {
            Toast.makeText(this, "No pending deposit requests!", Toast.LENGTH_SHORT).show()
            return
        }

        val items = pendingDeposits.map { (phone, _, dep) ->
            "$phone | ৳${dep.amount}\nGateway: ${dep.wallet} | TxID: ${dep.txid}"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Pending Deposits")
            .setItems(items) { _, which ->
                val (phone, id, dep) = pendingDeposits[which]
                AlertDialog.Builder(this)
                    .setTitle("Deposit: ৳${dep.amount} from $phone")
                    .setMessage("Sender: ${dep.sender}\nTxID: ${dep.txid}\nDate: ${dep.date}")
                    .setPositiveButton("Approve") { _, _ ->
                        repository.approveDeposit(phone, id, dep.amount ?: 0.0) {
                            Toast.makeText(this, "Deposit approved!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Reject") { _, _ ->
                        repository.rejectDeposit(phone, id) {
                            Toast.makeText(this, "Deposit rejected!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .show()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    // ==========================================
    // ৩. উইথড্রয়াল অনুমোদন ডায়ালগ
    // ==========================================
    private fun showWithdrawalsDialog() {
        val pendingWd = mutableListOf<Triple<String, String, Withdrawal>>()
        allUsers.forEach { (phone, user) ->
            user.withdrawals?.forEach { (id, wd) ->
                if (wd.status == "Pending") pendingWd.add(Triple(phone, id, wd))
            }
        }

        if (pendingWd.isEmpty()) {
            Toast.makeText(this, "No pending withdrawal requests!", Toast.LENGTH_SHORT).show()
            return
        }

        val items = pendingWd.map { (phone, _, wd) ->
            "$phone | ৳${wd.amount} | Gateway: ${wd.wallet}"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Pending Withdrawals")
            .setItems(items) { _, which ->
                val (phone, id, wd) = pendingWd[which]
                AlertDialog.Builder(this)
                    .setTitle("Cash Out: ৳${wd.amount}")
                    .setMessage("User: $phone\nWallet: ${wd.wallet}\nDate: ${wd.date}")
                    .setPositiveButton("Approve") { _, _ ->
                        repository.approveWithdrawal(phone, id) {
                            Toast.makeText(this, "Withdrawal approved!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Reject (Refund)") { _, _ ->
                        repository.rejectWithdrawal(phone, id, wd.amount ?: 0.0) {
                            Toast.makeText(this, "Rejected and refunded to user!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .show()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    // ==========================================
    // ৪. সেন্ডমানি ডায়ালগ
    // ==========================================
    private fun showSendMoneyDialog() {
        val pendingList = pendingSendMoneyMap.filter { it.value.status == "Pending" }.toList()
        if (pendingList.isEmpty()) {
            Toast.makeText(this, "No pending send money requests!", Toast.LENGTH_SHORT).show()
            return
        }

        val items = pendingList.map { (id, item) ->
            "${item.sender} -> ${item.target} (৳${item.amount})\nFee: ৳${item.fee} | Total: ৳${item.totalDeducted}"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Send Money Requests")
            .setItems(items) { _, which ->
                val (id, item) = pendingList[which]
                AlertDialog.Builder(this)
                    .setTitle("Transfer ৳${item.amount}")
                    .setMessage("From: ${item.sender}\nTo: ${item.target}")
                    .setPositiveButton("Approve") { _, _ ->
                        repository.approveSendMoney(id, item.sender ?: "", item.target ?: "", item.amount ?: 0.0, item.senderName ?: "") {
                            Toast.makeText(this, "Send money approved!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Reject (Refund)") { _, _ ->
                        repository.rejectSendMoney(id, item.sender ?: "", item.totalDeducted ?: 0.0) {
                            Toast.makeText(this, "Rejected & refunded to sender!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .show()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    // ==========================================
    // ৫. মোবাইল রিচার্জ ডায়ালগ
    // ==========================================
    private fun showRechargesDialog() {
        val pendingRecharges = mutableListOf<Triple<String, String, Recharge>>()
        allUsers.forEach { (phone, user) ->
            user.recharges?.forEach { (id, rc) ->
                if (rc.status == "Pending") pendingRecharges.add(Triple(phone, id, rc))
            }
        }

        if (pendingRecharges.isEmpty()) {
            Toast.makeText(this, "No pending recharge requests!", Toast.LENGTH_SHORT).show()
            return
        }

        val items = pendingRecharges.map { (phone, _, rc) ->
            "$phone -> ${rc.number} (${rc.operator} - ${rc.type})\nAmount: ৳${rc.amount}"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Pending Recharges")
            .setItems(items) { _, which ->
                val (phone, id, rc) = pendingRecharges[which]
                AlertDialog.Builder(this)
                    .setTitle("Recharge ৳${rc.amount} to ${rc.number}")
                    .setMessage("Operator: ${rc.operator} (${rc.type})\nRequested by: $phone")
                    .setPositiveButton("Approve") { _, _ ->
                        repository.approveRecharge(phone, id) {
                            Toast.makeText(this, "Recharge approved!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Reject (Refund)") { _, _ ->
                        repository.rejectRecharge(phone, id, rc.amount ?: 0.0) {
                            Toast.makeText(this, "Rejected and refunded!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .show()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    // ==========================================
    // ৬. গিফট ভাউচার জেনারেটর ডায়ালগ (২১-ডিজিট সিকিউর কোড)
    // ==========================================
    private fun showGiftVoucherDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }
        val etClaimer = EditText(this).apply { hint = "Claimer User UID (6 Digits)" }
        val etReferred = EditText(this).apply { hint = "Referred User UID (6 Digits)" }
        val etAmount = EditText(this).apply { hint = "Bonus Amount (৳)" }

        layout.addView(etClaimer)
        layout.addView(etReferred)
        layout.addView(etAmount)

        AlertDialog.Builder(this)
            .setTitle("Generate 21-Digit Gift Voucher")
            .setView(layout)
            .setPositiveButton("Generate Code") { _, _ ->
                val claimerUid = etClaimer.text.toString().trim()
                val referredUid = etReferred.text.toString().trim()
                val amt = etAmount.text.toString().toDoubleOrNull()

                if (claimerUid.length == 6 && referredUid.length == 6 && amt != null && amt > 0) {
                    repository.generateSecureGiftVoucher(claimerUid, referredUid, amt) { code ->
                        AlertDialog.Builder(this)
                            .setTitle("Voucher Generated Successfully!")
                            .setMessage("21-Digit Code:\n$code")
                            .setPositiveButton("OK", null)
                            .show()
                    }
                } else {
                    Toast.makeText(this, "Please enter valid 6-digit UIDs and amount!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ==========================================
    // ৭. টাইপিং টাস্ক অনুমোদন ডায়ালগ (নিট লাভের ৫% কমিশন ও রাত ১২টার লক)
    // ==========================================
    private fun showTypingTasksDialog() {
        val pendingTasks = mutableListOf<Triple<String, String, TypingTask>>()
        allUsers.forEach { (phone, user) ->
            user.paragraph_jobs?.forEach { (id, task) ->
                if (task.status == "Pending") pendingTasks.add(Triple(phone, id, task))
            }
        }

        if (pendingTasks.isEmpty()) {
            Toast.makeText(this, "No pending typing tasks!", Toast.LENGTH_SHORT).show()
            return
        }

        val items = pendingTasks.map { (phone, _, task) ->
            "$phone | Topic: \"${task.topic}\"\nReward: ৳${task.amount} | Fee: ৳${task.entry_fee}"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Pending Typing Tasks")
            .setItems(items) { _, which ->
                val (phone, id, task) = pendingTasks[which]
                AlertDialog.Builder(this)
                    .setTitle("Review Task: ${task.topic}")
                    .setMessage("User Text:\n${task.text}\n\nReward: ৳${task.amount} | Fee: ৳${task.entry_fee}")
                    .setPositiveButton("Approve") { _, _ ->
                        repository.approveTypingTask(phone, id, task.amount ?: 0.0, task.entry_fee ?: 0.0, allUsers) { comm ->
                            Toast.makeText(this, "Task approved! Referrer earned 5% net commission (৳$comm)", Toast.LENGTH_LONG).show()
                        }
                    }
                    .setNegativeButton("Reject (Refund)") { _, _ ->
                        repository.rejectTypingTask(phone, id, task.entry_fee ?: 0.0, "Writing does not meet criteria") {
                            Toast.makeText(this, "Task rejected and fee refunded!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .show()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    // ==========================================
    // ৮. সাপোর্ট চ্যাট ডায়ালগ
    // ==========================================
    private fun showSupportChatDialog() {
        db.child("chats").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val chatUsers = snapshot.children.mapNotNull { it.key }.toTypedArray()
                if (chatUsers.isEmpty()) {
                    Toast.makeText(this@MainActivity, "No chat threads available!", Toast.LENGTH_SHORT).show()
                    return
                }

                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Inbox Conversations")
                    .setItems(chatUsers) { _, which ->
                        openChatWithUser(chatUsers[which])
                    }
                    .setNegativeButton("Close", null)
                    .show()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun openChatWithUser(phone: String) {
        val input = EditText(this).apply { hint = "Type admin reply..." }
        AlertDialog.Builder(this)
            .setTitle("Chat with: $phone")
            .setView(input)
            .setPositiveButton("Send Reply") { _, _ ->
                val text = input.text.toString().trim()
                if (text.isNotEmpty()) {
                    repository.sendAdminChatMessage(phone, text) {
                        Toast.makeText(this, "Reply sent!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ==========================================
    // ৯. ব্যালেন্স রিসেট কন্ট্রোলার (সিঙ্গেল ও বালক ০.০০)
    // ==========================================
    private fun showBalanceResetDialog() {
        val options = arrayOf(
            "1. Reset Specific User (by Phone/UID)",
            "2. ⚠️ RESET ALL USERS TO ৳0.00 (Bulk Reset)"
        )

        AlertDialog.Builder(this)
            .setTitle("Balance Reset Controller")
            .setItems(options) { _, which ->
                if (which == 0) {
                    val input = EditText(this).apply { hint = "Enter Phone or 6-digit UID" }
                    AlertDialog.Builder(this)
                        .setTitle("Reset User Balance")
                        .setView(input)
                        .setPositiveButton("Reset") { _, _ ->
                            val q = input.text.toString().trim()
                            val targetPhone = if (allUsers.containsKey(q)) q else allUsers.entries.find { it.value.uid == q }?.key
                            if (targetPhone != null) {
                                repository.resetSingleUserBalance(targetPhone) {
                                    Toast.makeText(this, "Balance reset to ৳0.00 for $targetPhone", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(this, "User not found!", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                } else {
                    val confirmInput = EditText(this).apply { hint = "Type \"00\" to confirm" }
                    AlertDialog.Builder(this)
                        .setTitle("⚠️ BULK BALANCE RESET")
                        .setMessage("Warning: This will permanently set ALL (${allUsers.size}) registered users' balance to ৳0.00!")
                        .setView(confirmInput)
                        .setPositiveButton("RESET ALL") { _, _ ->
                            if (confirmInput.text.toString().trim() == "00") {
                                repository.resetAllUsersBalance(allUsers.keys.toList()) { success ->
                                    if (success) Toast.makeText(this, "All users' balance reset to ৳0.00!", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(this, "Cancelled: Code mismatch!", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    // ==========================================
    // ১০. সিস্টেম সেটিংস ডায়ালগ
    // ==========================================
    private fun showSystemSettingsDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }
        val etBkash = EditText(this).apply { hint = "bKash Personal No." }
        val etNagad = EditText(this).apply { hint = "Nagad Personal No." }
        val etRocket = EditText(this).apply { hint = "Rocket Personal No." }
        val etUpay = EditText(this).apply { hint = "Upay Personal No." }

        layout.addView(etBkash)
        layout.addView(etNagad)
        layout.addView(etRocket)
        layout.addView(etUpay)

        AlertDialog.Builder(this)
            .setTitle("System MFS Gateways")
            .setView(layout)
            .setPositiveButton("Save Settings") { _, _ ->
                val gateways = mapOf(
                    "bKash" to mapOf("number" to etBkash.text.toString(), "label" to "bKash Personal"),
                    "Nagad" to mapOf("number" to etNagad.text.toString(), "label" to "Nagad Personal"),
                    "Rocket" to mapOf("number" to etRocket.text.toString(), "label" to "Rocket Personal"),
                    "Upay" to mapOf("number" to etUpay.text.toString(), "label" to "Upay Personal")
                )
                repository.saveSystemSettings(gateways, mapOf("telegram" to "https://t.me/OnlineBD77R"), 6) {
                    Toast.makeText(this, "Gateways saved successfully!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
