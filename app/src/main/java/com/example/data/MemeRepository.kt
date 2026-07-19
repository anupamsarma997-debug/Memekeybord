package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class MemeRepository(private val memeDao: MemeDao) {

    val allDialogues: Flow<List<MemeDialogue>> = memeDao.getAllDialogues()

    suspend fun insert(dialogue: MemeDialogue) {
        memeDao.insertDialogue(dialogue)
    }

    suspend fun update(dialogue: MemeDialogue) {
        memeDao.updateDialogue(dialogue)
    }

    suspend fun deleteById(id: Int) {
        memeDao.deleteDialogueById(id)
    }

    suspend fun checkAndPrepopulate() {
        val currentList = memeDao.getAllDialogues().first()
        if (currentList.isEmpty()) {
            memeDao.insertDialogues(getDefaultDialogues())
        }
    }

    suspend fun resetToDefault() {
        memeDao.deleteAll()
        memeDao.insertDialogues(getDefaultDialogues())
    }

    fun getDefaultDialogues(): List<MemeDialogue> {
        return listOf(
            // 😂 Hasna
            MemeDialogue(emoji = "😂", dialogue = "Yeh le, 150 rupiya dega is joke ka!", category = "😂 Hasna", mood = "FUNNY"),
            MemeDialogue(emoji = "🤣", dialogue = "Hans mat pagli, pyaar ho jayega!", category = "😂 Hasna", mood = "FUNNY"),
            MemeDialogue(emoji = "😁", dialogue = "Badi daant nikal raha hai, koi lottery lagi hai kya?", category = "😂 Hasna", mood = "FUNNY"),
            MemeDialogue(emoji = "😅", dialogue = "Bade aaram se phas gaya yeh toh!", category = "😂 Hasna", mood = "FUNNY"),
            MemeDialogue(emoji = "😜", dialogue = "Tu to chupa rustam nikla!", category = "😂 Hasna", mood = "FUNNY"),

            // 😎 Swag
            MemeDialogue(emoji = "😎", dialogue = "Apna time aayega! Tera bhai kisi se kam nahi.", category = "😎 Swag", mood = "SWAG"),
            MemeDialogue(emoji = "💅", dialogue = "Hawa aane de bhai, hawa aane de.", category = "😎 Swag", mood = "SWAG"),
            MemeDialogue(emoji = "🤑", dialogue = "Paisa laya? Pehle paisa nikal!", category = "😎 Swag", mood = "SWAG"),
            MemeDialogue(emoji = "🫡", dialogue = "Tumhe ekistopoki salami!", category = "😎 Swag", mood = "SWAG"),

            // 😭 Dukh
            MemeDialogue(emoji = "😭", dialogue = "Yeh dukh kaahe khatam nahi hota be!", category = "😭 Dukh", mood = "SAD"),
            MemeDialogue(emoji = "🤦‍♂️", dialogue = "Utha le re baba, mereko nahi, inko utha le!", category = "😭 Dukh", mood = "SAD"),
            MemeDialogue(emoji = "🤡", dialogue = "Sab milke humko pagal bana rahe hain!", category = "😭 Dukh", mood = "SAD"),
            MemeDialogue(emoji = "💀", dialogue = "Maut ka khel hai babu bhaiya, maut ka khel.", category = "😭 Dukh", mood = "SAD"),
            MemeDialogue(emoji = "🥺", dialogue = "Bhai pighal gaya...", category = "😭 Dukh", mood = "SAD"),
            MemeDialogue(emoji = "🫠", dialogue = "Haye m toh pighal gya", category = "😭 Dukh", mood = "SAD"),

            // 😱 Shock
            MemeDialogue(emoji = "😱", dialogue = "O Bhaai! Maaro mujhe maaro!", category = "😱 Shock", mood = "SHOCK"),
            MemeDialogue(emoji = "🤯", dialogue = "Bhai bhai kya baat boli hai", category = "😱 Shock", mood = "SHOCK"),
            MemeDialogue(emoji = "😳", dialogue = "Aise na dekh pagli, kuch kuch hota hai", category = "😱 Shock", mood = "SHOCK"),
            MemeDialogue(emoji = "🤩", dialogue = "Sahi pakde hain", category = "😱 Shock", mood = "SHOCK"),

            // ❤️ Pyaar
            MemeDialogue(emoji = "😍", dialogue = "Dil garden garden ho gaya!", category = "❤️ Pyaar", mood = "LOVE"),
            MemeDialogue(emoji = "😘", dialogue = "Ek pappi idhar, ek pappi udhar!", category = "❤️ Pyaar", mood = "LOVE"),
            MemeDialogue(emoji = "💔", dialogue = "Thukra ke mera pyaar, mera inteqam dekhegi.", category = "❤️ Pyaar", mood = "LOVE"),

            // 😡 Gussa
            MemeDialogue(emoji = "😡", dialogue = "Aata majhi satakli!", category = "😡 Gussa", mood = "ANGRY"),
            MemeDialogue(emoji = "👋", dialogue = "Chal nikal, pehli fursat mein nikal!", category = "😡 Gussa", mood = "ANGRY"),
            MemeDialogue(emoji = "🙏", dialogue = "Maaf kar de bhai, main haath jodta hoon.", category = "😡 Gussa", mood = "ANGRY"),
            MemeDialogue(emoji = "🙄", dialogue = "Paka mat yaar, aage badh.", category = "😡 Gussa", mood = "ANGRY"),
            MemeDialogue(emoji = "🥵", dialogue = "Gari nikal..", category = "😡 Gussa", mood = "ANGRY"),
            MemeDialogue(emoji = "😣", dialogue = "Ayi re, kya baat hai!", category = "😡 Gussa", mood = "ANGRY")
        )
    }
}
