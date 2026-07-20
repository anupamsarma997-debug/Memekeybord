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
            MemeDialogue(emoji = "🤑", dialogue = "Paisa hi paisa hoga, mast scheme hai!", category = "😂 Hasna", mood = "FUNNY"),
            MemeDialogue(emoji = "🤪", dialogue = "Are mujhe chakkar aane laga hai re baba!", category = "😂 Hasna", mood = "FUNNY"),
            MemeDialogue(emoji = "🥸", dialogue = "Control majnu bhai control, varna danga ho jayega!", category = "😂 Hasna", mood = "FUNNY"),

            // 😎 Swag
            MemeDialogue(emoji = "😎", dialogue = "Apna time aayega! Tera bhai kisi se kam nahi.", category = "😎 Swag", mood = "SWAG"),
            MemeDialogue(emoji = "💅", dialogue = "Hawa aane de bhai, hawa aane de.", category = "😎 Swag", mood = "SWAG"),
            MemeDialogue(emoji = "🤑", dialogue = "Paisa laya? Pehle paisa nikal!", category = "😎 Swag", mood = "SWAG"),
            MemeDialogue(emoji = "🫡", dialogue = "Tumhe ekis-topon ki salami di jaati hai!", category = "😎 Swag", mood = "SWAG"),
            MemeDialogue(emoji = "🔥", dialogue = "Jalwa hai hamara yahan, koi rok nahi sakta!", category = "😎 Swag", mood = "SWAG"),
            MemeDialogue(emoji = "🦁", dialogue = "Jhukega nahi saala! Attitude dekh bhai ka!", category = "😎 Swag", mood = "SWAG"),
            MemeDialogue(emoji = "👑", dialogue = "Sher apna rasta khud banata hai aur bhai raj karta hai!", category = "😎 Swag", mood = "SWAG"),
            MemeDialogue(emoji = "🕶️", dialogue = "Tension nahi lene ka, chill rehne ka boss!", category = "😎 Swag", mood = "SWAG"),

            // 😭 Dukh
            MemeDialogue(emoji = "😭", dialogue = "Yeh dukh kaahe khatam nahi hota be!", category = "😭 Dukh", mood = "SAD"),
            MemeDialogue(emoji = "🤦‍♂️", dialogue = "Utha le re baba, mereko nahi, inko utha le!", category = "😭 Dukh", mood = "SAD"),
            MemeDialogue(emoji = "🤡", dialogue = "Sab milke humko pagal bana rahe hain saale!", category = "😭 Dukh", mood = "SAD"),
            MemeDialogue(emoji = "💀", dialogue = "Maut ka khel hai babu bhaiya, maut ka khel.", category = "😭 Dukh", mood = "SAD"),
            MemeDialogue(emoji = "🥺", dialogue = "Bhai pighal gaya... dil tut gaya mera.", category = "😭 Dukh", mood = "SAD"),
            MemeDialogue(emoji = "🫠", dialogue = "Haye main toh pighal gaya is baat pe!", category = "😭 Dukh", mood = "SAD"),
            MemeDialogue(emoji = "💔", dialogue = "Dil ke armaan aansuon mein beh gaye!", category = "😭 Dukh", mood = "SAD"),
            MemeDialogue(emoji = "🌧️", dialogue = "Zindagi mein dukh dard ke siva kuch bacha hi nahi hai!", category = "😭 Dukh", mood = "SAD"),

            // 😱 Shock
            MemeDialogue(emoji = "😱", dialogue = "O Bhaai! Maaro mujhe maaro!", category = "😱 Shock", mood = "SHOCK"),
            MemeDialogue(emoji = "🤯", dialogue = "Bhai bhai kya baat boli hai! Dimag hila diya!", category = "😱 Shock", mood = "SHOCK"),
            MemeDialogue(emoji = "😳", dialogue = "Aise na dekh pagli, kuch kuch hota hai!", category = "😱 Shock", mood = "SHOCK"),
            MemeDialogue(emoji = "🤩", dialogue = "Sahi pakde hain! Bilkul gazab!", category = "😱 Shock", mood = "SHOCK"),
            MemeDialogue(emoji = "👁️", dialogue = "Arey deva! Ye kya anarth ho gaya re!", category = "😱 Shock", mood = "SHOCK"),
            MemeDialogue(emoji = "🚨", dialogue = "Danga fasad hone wala hai, bhago re bhago!", category = "😱 Shock", mood = "SHOCK"),

            // ❤️ Pyaar
            MemeDialogue(emoji = "😍", dialogue = "Dil garden garden ho gaya!", category = "❤️ Pyaar", mood = "LOVE"),
            MemeDialogue(emoji = "😘", dialogue = "Ek pappi idhar, ek pappi udhar!", category = "❤️ Pyaar", mood = "LOVE"),
            MemeDialogue(emoji = "🌹", dialogue = "Thukra ke mera pyaar, mera inteqam dekhegi.", category = "❤️ Pyaar", mood = "LOVE"),
            MemeDialogue(emoji = "💖", dialogue = "Tum toh meri jaan ho yaar, tumhare bina kya hai mera!", category = "❤️ Pyaar", mood = "LOVE"),
            MemeDialogue(emoji = "😻", dialogue = "Pehli nazar mein hi tumse pyaar ho gaya tha!", category = "❤️ Pyaar", mood = "LOVE"),

            // 😡 Gussa
            MemeDialogue(emoji = "😡", dialogue = "Aata majhi satakli!", category = "😡 Gussa", mood = "ANGRY"),
            MemeDialogue(emoji = "👋", dialogue = "Chal nikal, pehli fursat mein nikal!", category = "😡 Gussa", mood = "ANGRY"),
            MemeDialogue(emoji = "🙏", dialogue = "Maaf kar de bhai, main haath jodta hoon ab paka mat!", category = "😡 Gussa", mood = "ANGRY"),
            MemeDialogue(emoji = "🙄", dialogue = "Paka mat yaar, aage badh dimag mat chat.", category = "😡 Gussa", mood = "ANGRY"),
            MemeDialogue(emoji = "🥵", dialogue = "Abe gaadi nikal! Bahut bol raha hai tu!", category = "😡 Gussa", mood = "ANGRY"),
            MemeDialogue(emoji = "🤬", dialogue = "Aisi thappad marenge ki dilli dikh jayegi!", category = "😡 Gussa", mood = "ANGRY"),

            // 📜 Shayari
            MemeDialogue(emoji = "📜", dialogue = "Koi acha lage toh use pyaar mat karna, uske liye apni neend kharab mat karna. Do din toh wo aayenge khushi se milne, teesre din kahenge intezar mat karna.", category = "📜 Shayari", mood = "SAD"),
            MemeDialogue(emoji = "✍️", dialogue = "Sikhta wohi hai jo galti karta hai, galti wohi karta hai jo manta hai. Manta wohi hai jo sacha hai, aur aaj kal aisa koi bacha nahi hai!", category = "📜 Shayari", mood = "SWAG"),
            MemeDialogue(emoji = "🥀", dialogue = "Beet rahi hai zindagi khushiyon ki talaash mein, kat rahi hain raatein tumhari yaad mein. Din toh yun hi guzar jaate hain hasi-khushi mein, par kambakht ye raatein meri khushi chheen leti hain.", category = "📜 Shayari", mood = "SAD"),
            MemeDialogue(emoji = "🔥", dialogue = "Hum wo darya hain jo apna rasta khud banate hain, log toh bas bhedchal mein piche chalte jaate hain!", category = "📜 Shayari", mood = "SWAG"),
            MemeDialogue(emoji = "🌟", dialogue = "M apno ki zindagi ki wo roshni hu jo andhere mein saath deti hai aur ujaale mein milkar gayab ho jaati hai.", category = "📜 Shayari", mood = "LOVE"),
            MemeDialogue(emoji = "❤️", dialogue = "Ishq ne ghalib nikamma kar diya, varna hum bhi aadmi the kaam ke!", category = "📜 Shayari", mood = "LOVE"),
            MemeDialogue(emoji = "🕊️", dialogue = "So accept that everything is written in destiny.", category = "📜 Shayari", mood = "SAD"),

            // 🤝 Dosti
            MemeDialogue(emoji = "🤝", dialogue = "Dost fail ho jaye toh dukh hota hai, par dost top kar jaye toh aur dukh hota hai!", category = "🤝 Dosti", mood = "FUNNY"),
            MemeDialogue(emoji = "🫂", dialogue = "Dosti mein no sorry, no thank you, bas thodi si masti aur dher saara pyaar!", category = "🤝 Dosti", mood = "LOVE"),
            MemeDialogue(emoji = "👬", dialogue = "Arey bhai tu toh mera sabse bada yaar hai, tere liye toh jaan bhi hazir hai!", category = "🤝 Dosti", mood = "SWAG"),
            MemeDialogue(emoji = "🤙", dialogue = "Dost toh dost hota hai, chahe kaisa bhi ho, har musibat mein sabse pehle khada hota hai!", category = "🤝 Dosti", mood = "SWAG")
        )
    }
}
