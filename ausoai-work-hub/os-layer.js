
(()=>{
'use strict';
const ROOT='AUSOAI_OS_V1';
const LANG_KEY=ROOT+':lang';
const SUPPORTED=[['ko','한국어'],['en','English'],['ja','日本語'],['zh-CN','中文'],['vi','Tiếng Việt'],['th','ไทย'],['ru','Русский'],['hi','हिन्दी'],['id','Bahasa Indonesia'],['fr','Français'],['de','Deutsch'],['it','Italiano'],['es','Español']];
let ready=false, translating=false;

function ensureStyle(){
 if(document.getElementById('auso-os-style')) return;
 const st=document.createElement('style'); st.id='auso-os-style';
 st.textContent=`
 #ausoOsShell{position:fixed;z-index:2147483000;left:50%;bottom:calc(8px + env(safe-area-inset-bottom));transform:translateX(-50%);width:min(760px,calc(100% - 16px));display:flex;gap:6px;align-items:center;padding:6px 8px;border:1px solid rgba(255,255,255,.9);border-radius:17px;background:rgba(247,255,252,.94);box-shadow:0 12px 34px rgba(16,72,67,.22);backdrop-filter:blur(16px);font-family:system-ui,-apple-system,"Noto Sans KR",sans-serif}
 #ausoOsShell .brand{font-size:9px;font-weight:900;color:#087c70;white-space:nowrap}
 #ausoOsShell button,#ausoOsShell select{height:34px;border:1px solid #d5e9e3;border-radius:10px;background:#fff;color:#204440;font-size:10px;font-weight:750;padding:0 8px}
 #ausoOsShell select{flex:1;min-width:86px}
 #ausoOsShell button.active{background:#087c70;color:#fff;border-color:#087c70}
 #ausoOsState{font-size:9px;color:#607c78;white-space:nowrap;max-width:130px;overflow:hidden;text-overflow:ellipsis}
 #google_translate_element{position:fixed;left:-9999px;top:-9999px;width:1px;height:1px;overflow:hidden}
 body{padding-bottom:max(76px,env(safe-area-inset-bottom))}
 .goog-te-banner-frame.skiptranslate,.goog-te-balloon-frame{display:none!important}
 body{top:0!important}
 @media(max-width:560px){#ausoOsShell .brand,#ausoOsState{display:none}#ausoOsShell button,#ausoOsShell select{font-size:9px;padding:0 6px}}
 `;
 document.head.appendChild(st);
}
function ensureShell(){
 let shell=document.getElementById('ausoOsShell');
 if(shell) return shell;
 shell=document.createElement('div'); shell.id='ausoOsShell';
 shell.innerHTML='<span class="brand">AUSOAI OS</span><button id="ausoOsMic" type="button">🎙 음성</button><button id="ausoOsTranslate" type="button">화면 번역</button><select id="ausoOsLang" aria-label="화면 번역 언어"></select><button id="ausoOsOriginal" type="button">원문</button><span id="ausoOsState">OS 준비</span>';
 document.body.appendChild(shell);
 return shell;
}
function langSelect(){
 const s=document.getElementById('ausoOsLang');
 if(s && !s.options.length) SUPPORTED.forEach(([v,n])=>{const o=document.createElement('option');o.value=v;o.textContent=n;s.appendChild(o)});
 try{const saved=localStorage.getItem(LANG_KEY);if(saved&&SUPPORTED.some(x=>x[0]===saved))s.value=saved}catch{}
 return s;
}
function setState(t){const n=document.getElementById('ausoOsState');if(n)n.textContent=t}
function setGoogCookie(lang){
 const val=lang==='ko'?'/ko/ko':'/ko/'+lang;
 const exp=';path=/;SameSite=Lax';
 document.cookie='googtrans='+val+exp;
 try{document.cookie='googtrans='+val+';domain=.'+location.hostname+exp}catch{}
}
function combo(){
 return document.querySelector('.goog-te-combo');
}
function applyLanguage(lang){
 const s=langSelect(); if(s)s.value=lang;
 try{localStorage.setItem(LANG_KEY,lang)}catch{}
 if(lang==='ko'){
   setGoogCookie('ko');
   const c=combo(); if(c){c.value='ko';c.dispatchEvent(new Event('change'))}
   location.reload(); return;
 }
 setGoogCookie(lang);
 const c=combo();
 if(c){
   c.value=lang;c.dispatchEvent(new Event('change',{bubbles:true}));
   translating=true;
   document.getElementById('ausoOsTranslate')?.classList.add('active');
   setState('번역 '+lang);
 }else{
   setState('번역 엔진 준비 중');
   setTimeout(()=>applyLanguage(lang),450);
 }
}
window.googleTranslateElementInit=function(){
 try{
  new google.translate.TranslateElement({
    pageLanguage:'ko',
    includedLanguages:SUPPORTED.map(x=>x[0]).filter(x=>x!=='ko').join(','),
    autoDisplay:false,
    multilanguagePage:true
  },'google_translate_element');
  ready=true;
  const s=langSelect(); const saved=s?.value||'ko';
  if(saved!=='ko') setTimeout(()=>applyLanguage(saved),500); else setState('OS 준비');
 }catch(e){setState('번역 엔진 오류')}
};
function loadTranslate(){
 if(document.getElementById('google-translate-script')) return;
 const holder=document.createElement('div');holder.id='google_translate_element';document.body.appendChild(holder);
 const sc=document.createElement('script');sc.id='google-translate-script';sc.src='https://translate.google.com/translate_a/element.js?cb=googleTranslateElementInit';sc.async=true;document.head.appendChild(sc);
}
function speakCommand(){
 const SR=window.SpeechRecognition||window.webkitSpeechRecognition;
 if(!SR){setState('음성 미지원');return}
 const r=new SR();r.lang='ko-KR';r.interimResults=false;r.maxAlternatives=1;
 const b=document.getElementById('ausoOsMic');b?.classList.add('active');setState('듣는 중');
 r.onresult=e=>{
   const q=(e.results[0][0].transcript||'').trim();setState(q);
   const map=[['일본어','ja'],['영어','en'],['중국어','zh-CN'],['베트남어','vi'],['태국어','th'],['러시아어','ru'],['힌디어','hi'],['인도네시아어','id'],['프랑스어','fr'],['독일어','de'],['이탈리아어','it'],['스페인어','es']];
   const hit=map.find(x=>q.includes(x[0])&&/번역|바꿔|변경/.test(q));
   if(hit){applyLanguage(hit[1]);return}
   if(/원문|한국어로|번역.*꺼/.test(q)){applyLanguage('ko');return}
   if(/메일.*열/.test(q)){location.href='https://taeguad1-crypto.github.io/Mela1/ausoai-work-hub/mail.html';return}
   if(/편집기|문서.*편집/.test(q)){location.href='https://taeguad1-crypto.github.io/Mela1/ausoai-work-hub/editor.html';return}
   if(/카카오톡|카톡/.test(q)){location.href='intent://launch/#Intent;scheme=kakaotalk;package=com.kakao.talk;S.browser_fallback_url=https%3A%2F%2Fwww.kakaocorp.com%2Fpage%2Fservice%2Fservice%2FKakaoTalk;end';return}
   if(typeof window.__AUSOAI_COMMAND__==='function'){try{const handled=window.__AUSOAI_COMMAND__(q);if(handled)return}catch{}}
   const input=document.querySelector('#query,#cmd');if(input){input.value=q;input.dispatchEvent(new Event('input',{bubbles:true}));}
 };
 r.onerror=()=>setState('음성 인식 오류');
 r.onend=()=>b?.classList.remove('active');
 try{r.start()}catch{setState('마이크 다시 눌러 주세요')}
}
function bind(){
 ensureStyle();ensureShell();const s=langSelect();loadTranslate();
 document.getElementById('ausoOsTranslate').onclick=()=>applyLanguage(s.value==='ko'?'en':s.value);
 document.getElementById('ausoOsOriginal').onclick=()=>applyLanguage('ko');
 document.getElementById('ausoOsMic').onclick=speakCommand;
 s.onchange=()=>applyLanguage(s.value);
}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',bind);else bind();
window.AUSOAI_OS={applyLanguage,setState,speakCommand};
})();
