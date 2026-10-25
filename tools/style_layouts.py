"""Apply Diar's warm botanical visual system to the initial XML scaffold."""
from pathlib import Path
import xml.etree.ElementTree as E

R = Path('app/src/main/res')
A = '{http://schemas.android.com/apk/res/android}'
P = '{http://schemas.android.com/apk/res-auto}'
E.register_namespace('android', A)
E.register_namespace('app', P)
strings = E.parse(R/'values/strings.xml').getroot()
copy = {e.get('name'): e.text for e in strings}
def save(path, root):
    E.indent(root, space='    ')
    path.write_text(E.tostring(root, encoding='unicode')+'\n', encoding='utf-8')
def resource(key, value):
    if key not in copy:
        E.SubElement(strings,'string',name=key).text = value.replace("'", "\\'").replace('\n', '\\n')
        copy[key] = value
    return '@string/'+key
def node(view_type, **attrs):
    e = E.Element(view_type)
    for k,v in attrs.items():
        e.set('style' if k=='style' else A+k, str(v))
    return e
def col(**attrs):
    return node('LinearLayout',layout_width='match_parent',layout_height='wrap_content',orientation='vertical',**attrs)
def label(key, value, style='Body', **attrs):
    return node('TextView',style='@style/'+style,text=resource(key,value),**attrs)
def action(key, value, target, style='Action', **attrs):
    return node('com.google.android.material.button.MaterialButton',style='@style/'+style,text=resource(key,value),tag=target,onClick='navigate',**attrs)
def shape(name, color, radius=20, stroke=None, end=None):
    root = E.Element('shape')
    if end: E.SubElement(root,'gradient',{A+'startColor':color,A+'endColor':end,A+'angle':'315'})
    else: E.SubElement(root,'solid',{A+'color':color})
    E.SubElement(root,'corners',{A+'radius':f'{radius}dp'})
    if stroke: E.SubElement(root,'stroke',{A+'width':'1dp',A+'color':stroke})
    save(R/'drawable'/f'{name}.xml',root)

palette = {'teal':'#195D60','cream':'#F7F2E8','ink':'#173D3F','muted':'#60716B',
    'line':'#DFE6DE','white':'#FFFFFF','canvas':'#FAF8F2','sage':'#E0EBDD',
    'sage_dark':'#41664B','terracotta':'#A94E36','peach':'#F8E3D6',
    'gold':'#EBC46D','gold_light':'#F7EDCE','teal_light':'#DDEDE9','deep_teal':'#123F43'}
colors=E.Element('resources')
for k,v in palette.items(): E.SubElement(colors,'color',name=k).text=v
save(R/'values/colors.xml',colors)
for name,color,radius,stroke,end in [
    ('field','@color/white',16,'@color/line',None),('card','@color/white',24,None,None),
    ('pill','@color/teal_light',20,None,None),('cream','@color/cream',24,None,None),
    ('surface_sage','@color/sage',24,None,None),('surface_peach','@color/peach',24,None,None),
    ('surface_gold','@color/gold_light',24,None,None),('hero_teal','#195D60',28,None,'#2C7A70'),
    ('hero_sage','#E3EEDB',28,None,'#F4E9CA'),('nav_background','@color/white',26,'@color/line',None)]:
    shape(name,color,radius,stroke,end)

styles=E.Element('resources')
def style(name, parent=None, **items):
    e=E.SubElement(styles,'style',name=name,**({'parent':parent} if parent else {}))
    for key,val in items.items(): E.SubElement(e,'item',name=key.replace('__',':')).text=str(val)
style('Body',android__layout_width='match_parent',android__layout_height='wrap_content',android__textColor='@color/ink',android__textSize='15sp',android__lineSpacingExtra='4dp',android__layout_marginBottom='12dp')
style('Title','Body',android__textSize='32sp',android__fontFamily='sans-serif-medium',android__letterSpacing='-0.035',android__lineSpacingExtra='0dp',android__layout_marginBottom='16dp')
style('Subtitle','Body',android__textSize='19sp',android__fontFamily='sans-serif-medium',android__layout_marginTop='14dp')
style('Muted','Body',android__textColor='@color/muted',android__textSize='13sp')
style('Eyebrow','Body',android__textSize='11sp',android__textStyle='bold',android__letterSpacing='0.14',android__textColor='@color/terracotta',android__layout_marginBottom='10dp')
style('Field',android__layout_width='match_parent',android__layout_height='56dp',android__background='@drawable/field',android__padding='16dp',android__textSize='15sp',android__textColor='@color/ink',android__textColorHint='@color/muted',android__singleLine='true',android__layout_marginBottom='18dp')
style('Action','Widget.Material3.Button',android__layout_width='match_parent',android__layout_height='58dp',android__textAllCaps='false',android__textSize='14sp',android__fontFamily='sans-serif-medium',android__layout_marginTop='8dp',android__textColor='@color/white',android__insetTop='0dp',android__insetBottom='0dp',cornerRadius='18dp',backgroundTint='@color/teal',rippleColor='#30FFFFFF',android__elevation='0dp')
style('Secondary','Action',backgroundTint='@color/teal_light',android__textColor='@color/teal')
style('Warm','Action',backgroundTint='@color/peach',android__textColor='@color/terracotta')
style('Gold','Action',backgroundTint='@color/gold',android__textColor='@color/deep_teal')
style('Outline','Action',backgroundTint='@color/white',android__textColor='@color/ink',strokeColor='@color/line',strokeWidth='1dp')
style('TextAction','Action',backgroundTint='@android:color/transparent',android__textColor='@color/teal')
style('Photo','ShapeAppearance.Material3.Corner.Medium',cornerFamily='rounded',cornerSize='20dp')
save(R/'values/styles.xml',styles)

# Crisp vector icons replace font-dependent symbol glyphs.
icons={
    'home':'M3,10L12,3L21,10M5,9V21H10V15H14V21H19V9',
    'heart':'M12,21L3.5,12.5C-3,5.5 7,0 12,7C17,0 27,5.5 20.5,12.5Z',
    'search':'M10,3A7,7 0,1 0,10,17A7,7 0,1 0,10,3M15,15L22,22',
    'chat':'M3,3H21V17H9L3,22Z M7,8H17 M7,12H14',
    'account':'M12,3A4,4 0,1 0,12,11A4,4 0,1 0,12,3M4,22V19C4,12 20,12 20,19V22',
    'bell':'M5,17H19L17,14V9C17,2 7,2 7,9V14Z M10,21H14',
    'back':'M15,4L7,12L15,20',
    'check':'M5,12L10,17L20,6',
    'arrow':'M4,12H20M14,6L20,12L14,18'}
for name,path in icons.items():
    root=E.Element('vector',{A+'width':'24dp',A+'height':'24dp',A+'viewportWidth':'24',A+'viewportHeight':'24'})
    E.SubElement(root,'path',{A+'pathData':path,A+'strokeColor':'@color/teal',A+'strokeWidth':'1.7',A+'strokeLineCap':'round',A+'strokeLineJoin':'round',A+'fillColor':'@android:color/transparent'})
    save(R/'drawable'/f'ic_{name}.xml',root)

for path in sorted((R/'layout').glob('screen_*.xml')):
    root=E.parse(path).getroot()
    name=path.stem.removeprefix('screen_')
    root.set(A+'background','@color/canvas')
    body=root.find('ScrollView/LinearLayout')
    body.set(A+'padding','20dp')
    body.set(A+'paddingBottom','28dp')
    root.find('ScrollView').set(A+'scrollbars','none')
    # Shared header, field and button treatment.
    for e in list(root.iter()):
        if e.tag=='ImageView' and e.get(A+'src','').startswith('@drawable/house'):
            e.tag='com.google.android.material.imageview.ShapeableImageView'
            e.set(P+'shapeAppearanceOverlay','@style/Photo')
            e.set(A+'layout_marginBottom','12dp')
        if e.tag=='ImageView' and e.get(A+'src')=='@drawable/diar_logo':
            e.set(A+'background','@drawable/cream')
            e.set(A+'layout_marginBottom','20dp')
        if e.tag.endswith('MaterialButton'):
            target=e.get(A+'tag','')
            identifier=e.get(A+'id','')
            if identifier in ['@+id/google','@+id/facebook']: e.set('style','@style/Outline')
            elif identifier=='@+id/save_home': e.set('style','@style/Warm')
            elif target in ['search','map','favorites','tour']: e.set('style','@style/Secondary')
            elif target in ['signup','login','welcome','home'] and name not in ['splash','login']: e.set('style','@style/TextAction')
            elif target in ['detail','conversation','notifications','screens'] or name=='screens': e.set('style','@style/Outline')
            if target=='search':
                e.set(P+'icon','@drawable/ic_search'); e.set(P+'iconTint','@color/teal');e.set(P+'iconGravity','textStart')
        if e.get(A+'onClick')=='goBack':
            e.tag='ImageButton'; e.attrib.pop(A+'text',None);e.attrib.pop(A+'textSize',None)
            e.set(A+'src','@drawable/ic_back');e.set(A+'background','@drawable/pill');e.set(A+'padding','12dp')
        if e.get(A+'layout_weight')=='1' and e.get('style')=='@style/Subtitle':
            e.set(A+'layout_width','0dp');e.set(A+'layout_marginBottom','0dp');e.set(A+'layout_marginTop','0dp');e.set(A+'textSize','16sp')
    # Floating navigation with a genuine selected state, not five identical labels.
    nav=list(root)[-1]
    if nav.tag=='LinearLayout' and nav.get(A+'gravity')=='center':
        nav.set(A+'background','@drawable/nav_background');nav.set(A+'layout_marginHorizontal','16dp');nav.set(A+'layout_marginBottom','8dp');nav.set(A+'padding','6dp');nav.set(A+'layout_height','70dp');nav.set(A+'elevation','3dp')
        for e,icon,title in zip(nav,['home','heart','search','chat','account'],['Home','Saved','Discover','Inbox','Account']):
            active=e.get(A+'tag')==name
            e.set(A+'text',resource('nav_'+title.lower(),title));e.set(A+'drawableTop','@drawable/ic_'+icon)
            e.set(A+'drawablePadding','3dp');e.set(A+'paddingTop','5dp');e.set(A+'paddingBottom','3dp')
            if active: e.set(A+'background','@drawable/pill');e.set(A+'textStyle','bold')
    # Property cards: rounded photography, a colored price panel and calmer hierarchy.
    cards=[e for e in body.iter('LinearLayout') if e.get(A+'tag')=='detail']
    for i,card in enumerate(cards):
        card.set(A+'padding','10dp');card.set(A+'layout_marginBottom','20dp');card.set(A+'elevation','2dp')
        card.set(A+'foreground','?attr/selectableItemBackground')
        photo,price,title,meta=list(card)
        price.set(A+'textSize','17sp');price.set(A+'layout_marginTop','0dp')
        title.set(A+'layout_marginTop','4dp'); title.set(A+'layout_marginBottom','6dp')
        panel=col(background='@drawable/'+['surface_sage','surface_peach','surface_gold'][i%3],padding='14dp')
        for child in [title,price,meta]: card.remove(child);panel.append(child)
        card.append(panel)
    if name=='home':
        old=list(body)
        for e in old[:8]: body.remove(e)
        top=node('LinearLayout',layout_width='match_parent',layout_height='56dp',gravity='center_vertical',layout_marginBottom='16dp')
        top.append(label('home_wordmark','DIAR  /  ديار','Subtitle',layout_width='0dp',layout_weight='1',layout_marginTop='0dp',layout_marginBottom='0dp',textSize='25sp'))
        top.append(node('ImageButton',layout_width='48dp',layout_height='48dp',src='@drawable/ic_bell',background='@drawable/surface_peach',padding='12dp',contentDescription=resource('notifications_button','Notifications, 3 new'),tag='notifications',onClick='navigate'))
        hero=col(background='@drawable/hero_teal',padding='24dp',layout_marginBottom='16dp')
        hero.append(label('home_eyebrow','ROOTED IN POSSIBILITY','Eyebrow',textColor='@color/gold'))
        hero.append(label('home_headline','Find your place.\nFeel at home.','Title',textColor='@color/white',textSize='34sp'))
        hero.append(label('home_description','Beautiful spaces. A brighter beginning.','Body',textColor='@color/teal_light'))
        hero.append(action('home_search','Where would you love to live?','search','Outline',textSize='13sp'))
        chips=node('LinearLayout',layout_width='match_parent',layout_height='wrap_content',layout_marginBottom='16dp')
        for i,(title,style_name) in enumerate([('Homes','Action'),('Villas','Warm'),('Apartments','Gold')]):
            chips.append(action('category_'+title.lower(),title,'listings',style_name,layout_width='0dp',layout_weight='1',layout_height='48dp',layout_marginEnd='6dp',textSize='12sp'))
        for i,e in enumerate([top,hero,chips,label('home_popular','A few places to fall for','Subtitle'),*cards]): body.insert(i,e)
    elif name=='welcome':
        hero=col(background='@drawable/hero_sage',padding='20dp',layout_marginBottom='12dp')
        items=list(body)
        for e in items[1:4]: body.remove(e);hero.append(e)
        hero.insert(0,label('welcome_eyebrow','YOUR NEXT CHAPTER STARTS HERE','Eyebrow'))
        body.insert(1,hero)
        body.find('ImageView').set(A+'layout_height','130dp')
        for e in body:
            if e.get(A+'tag')=='login': e.set('style','@style/Action')
    elif name in ['login','signup','payment']:
        banner=col(background='@drawable/'+('hero_teal' if name=='payment' else 'hero_sage'),padding='20dp',layout_marginBottom='20dp')
        banner.append(label(name+'_eyebrow',{'login':'WELCOME HOME','signup':'A FRESH BEGINNING','payment':'DIAR  /  YOUR NEXT CHAPTER'}[name],'Eyebrow',textColor='@color/gold' if name=='payment' else '@color/terracotta'))
        if name=='payment':
            banner.append(label('payment_card_preview','A place of your own.','Subtitle',textColor='@color/white'))
            banner.append(label('payment_card_sample','••••   ••••   ••••   4242','Body',textColor='@color/teal_light'))
        else:
            for e in list(body):
                if e.get('style') in ['@style/Title','@style/Muted']:
                    body.remove(e);banner.append(e)
                    if e.get('style')=='@style/Muted':break
        body.insert(0,banner)
    elif name in ['success','search_empty','splash']:
        body.set(A+'gravity','center_horizontal')
        for e in body:
            if e.tag=='Space':e.set(A+'layout_height','30dp')
            if e.get(A+'textSize') in ['80sp','86sp']:
                e.tag='ImageView';e.attrib.pop(A+'text',None);e.attrib.pop(A+'textSize',None)
                e.set(A+'src','@drawable/ic_'+('check' if name=='success' else 'search'))
                e.set(A+'layout_width','112dp');e.set(A+'layout_height','112dp');e.set(A+'padding','32dp')
                e.set(A+'layout_marginTop','48dp');e.set(A+'layout_marginBottom','28dp');e.set(A+'background','@drawable/surface_sage')
                e.set(A+'contentDescription',resource('status_'+name,'Booking complete' if name=='success' else 'No results'))
        if name=='splash':
            body.set(A+'background','@drawable/hero_sage')
            body.insert(2,label('splash_eyebrow','ROOTED HERE. READY FOR TOMORROW.','Eyebrow',gravity='center'))
    elif name=='discover':
        hero=col(background='@drawable/hero_sage',padding='16dp',layout_marginBottom='16dp')
        for e in list(body)[:3]:body.remove(e);hero.append(e)
        hero.insert(0,label('discover_eyebrow','SOMEWHERE NEW. SOMETHING YOU.','Eyebrow'))
        body.insert(0,hero)
    elif name=='notifications':
        items=body.find("LinearLayout[@"+A+"id='@+id/notification_items']")
        children=list(items)
        for e in children:items.remove(e)
        for i in range(0,len(children),2):
            card=col(background='@drawable/'+['surface_sage','surface_peach','surface_gold'][i//2],padding='18dp',layout_marginTop='12dp')
            card.extend(children[i:i+2]);items.append(card)
    elif name in ['chat','account','screens']:
        buttons=[e for e in body if e.tag.endswith('MaterialButton')]
        for i,e in enumerate(buttons):
            e.set('style','@style/'+['Secondary','Warm','Outline','Gold'][i%4])
            e.set(A+'gravity','center_vertical|start');e.set(A+'paddingHorizontal','18dp')
            e.set(A+'layout_height','72dp' if name=='chat' else '58dp')
            if name=='chat':e.set(A+'maxLines','2')
    elif name=='detail':
        for e in body:
            value=copy.get(e.get(A+'text','').replace('@string/',''),'') or ''
            if 'bedrooms' in value or '356 reviews' in value:
                e.set(A+'background','@drawable/surface_gold' if 'reviews' in value else '@drawable/surface_sage')
                e.set(A+'padding','14dp');e.set(A+'textColor','@color/ink')
            if e.get(A+'tag')=='payment':e.set('style','@style/Gold')
    save(path,root)

save(R/'values/strings.xml',strings)
theme=E.parse(R/'values/themes.xml').getroot()
base=theme.find('style')
for k,v in {'colorSecondary':'@color/terracotta','colorTertiary':'@color/sage_dark','colorSurface':'@color/canvas','android:windowBackground':'@color/canvas'}.items():
    match=base.find(f"item[@name='{k}']")
    if match is None:match=E.SubElement(base,'item',name=k)
    match.text=v
save(R/'values/themes.xml',theme)
print('Applied the Diar botanical palette and screen styling.')
