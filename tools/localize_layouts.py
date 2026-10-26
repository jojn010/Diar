"""Generate complete Arabic resources and RTL-friendly XML from the Diar scaffold."""
from pathlib import Path
import xml.etree.ElementTree as E
R=Path('app/src/main/res')
A='{http://schemas.android.com/apk/res/android}'
E.register_namespace('android',A)
E.register_namespace('app','http://schemas.android.com/apk/res-auto')
def save(path,root):
    path.parent.mkdir(parents=True,exist_ok=True)
    E.indent(root,space='    ')
    path.write_text(E.tostring(root,encoding='unicode')+'\n',encoding='utf-8')
translations={
'Diar':'ديار','Go back':'رجوع','Account':'الحساب','Diar — بناء مستقبل مشرق':'ديار — بناء مستقبل مشرق',
'Welcome to Diar':'أهلاً بك في ديار','You are exploring the local demo.':'أنت تتصفح النسخة التجريبية.',
'My saved homes':'منازلي المحفوظة','Notifications':'الإشعارات','All prototype screens':'جميع الشاشات التجريبية',
'Back to welcome':'العودة إلى الترحيب','Home':'الرئيسية','Saved':'المحفوظات','Discover':'اكتشف','Inbox':'الرسائل',
'⌂ Home':'⌂ الرئيسية','♡ Saved':'♡ المحفوظات','⌕ Discover':'⌕ اكتشف','☏ Inbox':'☏ الرسائل','◎ Account':'◎ الحساب',
'Conversations':'المحادثات','Your next home starts with a hello.':'منزلك القادم يبدأ بمحادثة.',
'Diar Homes · See you on Sunday!':'ديار للعقارات · نراك يوم الأحد!',
'Lina Mansour · The villa is still available':'لينا منصور · الفيلا ما زالت متاحة',
'Omar Khalil · Let’s arrange a viewing':'عمر خليل · لنحدد موعداً للزيارة',
'Sara Nasser · Thank you for your interest':'سارة ناصر · شكراً لاهتمامك',
'Chat':'محادثة','Diar Homes':'ديار للعقارات','Property specialist • Demo conversation':'خبير عقاري • محادثة تجريبية',
'Hello! How can we help you find your next home?':'مرحباً! كيف نساعدك في العثور على منزلك القادم؟',
'Your message':'رسالتك','Ask about this property':'استفسر عن هذا العقار','Send message':'إرسال الرسالة',
'Property details':'تفاصيل العقار','Diar property exterior':'الواجهة الخارجية لعقار ديار',
'♡ Save this home':'♡ حفظ هذا المنزل','HOME ★ 4.8 · 356 reviews':'منزل ★ ٤٫٨ · ٣٥٦ تقييماً',
'Olive Garden Residence':'منزل حديقة الزيتون','⌖ Jerusalem':'⌖ القدس',
'3 bedrooms · 2 bathrooms · 145 m²':'٣ غرف نوم · حمّامان · ١٤٥ م²','About this home':'عن هذا المنزل',
'A welcoming home with sun-filled rooms, a private garden and generous spaces to gather. Discover the details and arrange a viewing with Diar.':'منزل دافئ بغرف تغمرها الشمس وحديقة خاصة ومساحات واسعة تجمع الأحبة. اكتشف التفاصيل واحجز زيارة مع ديار.',
'Explore the gallery':'استكشف الصور','Your property contact':'تواصل بشأن العقار','Diar Homes · Send a message':'ديار للعقارات · أرسل رسالة',
'$310,999 / year':'٣١٠٬٩٩٩ دولار / سنة','Rent now':'استأجر الآن',
'Discover your next address.':'اكتشف عنوانك القادم.','⌕ Search location':'⌕ ابحث عن موقع','Recent searches':'عمليات البحث الأخيرة',
'Jerusalem →':'القدس ←','Ramallah →':'رام الله ←','Explore the map':'استكشف الخريطة','Recommended for you':'اخترنا لك',
'$110,444 / year ★ 4.8':'١١٠٬٤٤٤ دولار / سنة ★ ٤٫٨','The Garden House':'منزل الحديقة',
'⌖ Jerusalem • 3 beds • 2 baths':'⌖ القدس • ٣ غرف نوم • حمّامان','Favorites':'المفضلة','Your saved spaces':'أماكنك المحفوظة',
'$310,999 / year ★ 4.8':'٣١٠٬٩٩٩ دولار / سنة ★ ٤٫٨','Tap the heart on a property to save or remove it.':'اضغط على القلب لحفظ العقار أو إزالته من المفضلة.',
'DIAR / ديار':'ديار / DIAR','Find your place to belong.':'اعثر على مكان يشبهك.','Notifications • 3 new':'الإشعارات • ٣ جديدة',
'⌕ Search a city or neighborhood':'⌕ ابحث عن مدينة أو حي','Home · Villa · Apartment':'منزل · فيلا · شقة','Popular homes':'منازل رائجة',
'$359,767 / year ★ 4.8':'٣٥٩٬٧٦٧ دولار / سنة ★ ٤٫٨','Sunlit Courtyard Villa':'فيلا الفناء المشرق','View all homes':'عرض جميع المنازل',
'Homes':'منازل','Homes selected for you':'منازل اخترناها لك','3 homes available • Demo listings':'٣ منازل متاحة • إعلانات تجريبية',
'Welcome back':'أهلاً بعودتك','Log in with your phone number':'سجّل الدخول برقم هاتفك',
'Enter your phone number to try the Diar prototype.':'أدخل رقم هاتفك لتجربة تطبيق ديار.','Phone number':'رقم الهاتف',
'Continue':'متابعة','Demo only. No verification code is sent.':'نسخة تجريبية فقط. لن يُرسل رمز تحقق.',
'Explore nearby':'استكشف ما حولك','⌕ Jerusalem':'⌕ القدس','3 listings in Jerusalem':'٣ عقارات في القدس',
'Illustrative map • Demo locations':'خريطة توضيحية • مواقع تجريبية','Illustrative neighborhood map':'خريطة توضيحية للحي',
'$310,999':'٣١٠٬٩٩٩ دولار','$359,767':'٣٥٩٬٧٦٧ دولار','$110,444':'١١٠٬٤٤٤ دولار',
'Recent notifications':'آخر الإشعارات','Clear all':'مسح الكل','● A home worth discovering':'● منزل يستحق الاكتشاف',
'New homes have just arrived in your area.':'أضيفت منازل جديدة في منطقتك.','⌂ Your next chapter awaits':'⌂ بدايتك الجديدة بانتظارك',
'Take a closer look at Olive Garden Residence.':'تعرّف أكثر على منزل حديقة الزيتون.','★ A little more to love':'★ المزيد مما تحب',
'Explore our highest-rated homes this week.':'استكشف المنازل الأعلى تقييماً هذا الأسبوع.',
'Add payment method':'إضافة وسيلة دفع','Payment details':'تفاصيل الدفع',
'Prototype checkout — use sample details only. No charge will be made.':'دفع تجريبي — استخدم بيانات وهمية فقط. لن يُخصم أي مبلغ.',
'Name on card':'الاسم على البطاقة','Sample name':'اسم تجريبي','Card number':'رقم البطاقة','Expiry (MM/YY)':'تاريخ الانتهاء (شهر/سنة)',
'CVC':'رمز الأمان','Country':'البلد','Palestine':'فلسطين','Complete demo payment':'إتمام الدفع التجريبي',
'Prototype screens':'الشاشات التجريبية','Diar screen gallery':'معرض شاشات ديار',
'01 Splash':'٠١ البداية','02 Welcome':'٠٢ الترحيب','03 Signup':'٠٣ إنشاء حساب','04 Login':'٠٤ تسجيل الدخول',
'05 Home':'٠٥ الرئيسية','06 Listings':'٠٦ العقارات','07 Notifications':'٠٧ الإشعارات','08 Tour':'٠٨ جولة العقار',
'09 Discover':'٠٩ اكتشف','10 Search':'١٠ البحث','11 Search Empty':'١١ لا توجد نتائج','12 Map':'١٢ الخريطة',
'13 Chat':'١٣ المحادثات','14 Detail':'١٤ التفاصيل','15 Payment':'١٥ الدفع','16 Success':'١٦ نجاح الدفع',
'Search':'البحث','Find a home':'ابحث عن منزل','Search Jerusalem, Ramallah, or a property':'ابحث عن القدس أو رام الله أو اسم عقار',
'Olive Garden Residence ↗':'منزل حديقة الزيتون ↖','Sunlit Courtyard Villa ↗':'فيلا الفناء المشرق ↖','The Garden House ↗':'منزل الحديقة ↖',
'Search by city or property name.':'ابحث باسم المدينة أو العقار.','Show on map':'عرض على الخريطة','No homes found':'لم نعثر على منازل',
'Try another city or neighborhood. More places are coming to Diar.':'جرّب مدينة أو حياً آخر. المزيد من الأماكن قريباً في ديار.',
'Try another search':'جرّب بحثاً آخر','Create account':'إنشاء حساب','Create an account and find a place of your own.':'أنشئ حساباً واعثر على مكانك الخاص.',
'Full name':'الاسم الكامل','Your full name':'اسمك الكامل','Email':'البريد الإلكتروني','Password':'كلمة المرور','At least 6 characters':'٦ أحرف على الأقل',
'I agree to the Terms and Privacy Policy':'أوافق على الشروط وسياسة الخصوصية','Sign up':'إنشاء حساب','Already have an account? Log in':'لديك حساب؟ سجّل الدخول',
'A place to belong.':'مكان تنتمي إليه.','Find your next chapter with Diar.':'ابدأ فصلك القادم مع ديار.','Get started':'ابدأ الآن',
'Payment success':'تم الدفع بنجاح','You’re all set.':'كل شيء جاهز.',
'Your demo booking is complete. Welcome to your next chapter with Diar.':'اكتمل حجزك التجريبي. أهلاً ببدايتك الجديدة مع ديار.',
'See my listing':'عرض عقاري','Back to home':'العودة للرئيسية','Property gallery':'صور العقار','A space to call yours':'مساحة تخصك',
'TYPE 45 / DIAR COLLECTION':'نموذج ٤٥ / مجموعة ديار',
'Natural light, open rooms and a quiet courtyard. Explore a thoughtfully designed home with space for every part of your day.':'ضوء طبيعي وغرف رحبة وفناء هادئ. استكشف منزلاً صُمّم بعناية ليحتضن كل تفاصيل يومك.',
'◇ 6 × 7.5 m • 3 bedrooms':'◇ ٦ × ٧٫٥ م • ٣ غرف نوم','View property details':'عرض تفاصيل العقار',
'Your home. Your next chapter.':'منزلك. بداية حكايتك.','Discover homes, apartments and spaces made for you.':'اكتشف منازل وشققاً ومساحات تناسبك.',
'Continue with mobile number':'المتابعة برقم الهاتف','Continue with Google':'المتابعة باستخدام Google','Continue with Facebook':'المتابعة باستخدام Facebook',
'New to Diar? Create an account':'جديد في ديار؟ أنشئ حساباً','Explore homes':'استكشف المنازل',
'SOMEWHERE NEW. SOMETHING YOU.':'مكان جديد يشبهك.','Notifications, 3 new':'الإشعارات، ٣ جديدة','ROOTED IN POSSIBILITY':'جذور راسخة وآفاق جديدة',
'Find your place.\\nFeel at home.':'اعثر على مكانك.\\nواشعر بالانتماء.',
'Beautiful spaces. A brighter beginning.':'مساحات جميلة. وبداية أكثر إشراقاً.','Where would you love to live?':'أين تحب أن تسكن؟',
'Villas':'فلل','Apartments':'شقق','A few places to fall for':'أماكن ستُحبها','WELCOME HOME':'أهلاً بك في منزلك',
'DIAR / YOUR NEXT CHAPTER':'ديار / بدايتك الجديدة','A place of your own.':'مكانك الخاص.','No results':'لا توجد نتائج',
'A FRESH BEGINNING':'بداية جديدة','ROOTED HERE. READY FOR TOMORROW.':'جذور هنا. وطموح للغد.','Booking complete':'اكتمل الحجز',
'YOUR NEXT CHAPTER STARTS HERE':'حكايتك القادمة تبدأ هنا',
}
unchanged={'‹','⌕','✓','+970 59 000 0000','4242 4242 4242 4242','12/30','123','you@example.com','•••• •••• •••• 4242'}
arabic=E.Element('resources')
missing=[]
for e in E.parse(R/'values/strings.xml').getroot():
    value=' '.join((e.text or '').split())
    translated=translations.get(value)
    if translated is None and value in unchanged:translated=value
    if translated is None:missing.append((e.get('name'),value));continue
    E.SubElement(arabic,'string',name=e.get('name')).text=translated
if missing:raise ValueError(f'Missing Arabic translations: {missing}')
save(R/'values-ar/strings.xml',arabic)

# Keep switches accessible before login as well as in Account.
for path in (R/'layout').glob('screen_*.xml'):
    root=E.parse(path).getroot()
    name=path.stem.removeprefix('screen_')
    if name in ['splash','welcome','account']:
        body=root.find('ScrollView/LinearLayout')
        button=E.Element('com.google.android.material.button.MaterialButton',{
            'style':'@style/Outline',A+'id':'@+id/change_language',A+'text':'@string/change_language'})
        body.insert(0 if name!='account' else 3,button)
    for e in root.iter():
        if e.tag=='EditText':
            kind=e.get(A+'inputType','')
            if kind in ['phone','number','numberPassword','textEmailAddress'] or e.get(A+'id')=='@+id/card_expiry':
                e.set(A+'textDirection','ltr');e.set(A+'gravity','start|center_vertical')
        if e.tag.endswith('MaterialButton'):
            e.set(A+'layout_height','wrap_content');e.set(A+'minHeight','56dp')
            e.set(A+'paddingTop','12dp');e.set(A+'paddingBottom','12dp')
    save(path,root)
for name in ['ic_back','ic_arrow']:
    path=R/'drawable'/f'{name}.xml';root=E.parse(path).getroot()
    root.set(A+'autoMirrored','true');save(path,root)
# Arabic needs connected letterforms and normal tracking.
styles=E.parse(R/'values/styles.xml').getroot()
for e in styles:
    if e.get('name') in ['Title','Eyebrow']:
        for item in e:
            if item.get('name')=='android:letterSpacing':item.text='0'
save(R/'values-ar/styles.xml',styles)
print(f'Arabic translations complete: {len(arabic)} strings.')
