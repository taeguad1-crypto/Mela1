(()=>{
  if(!/^https?:$/.test(location.protocol))return;
  const SpeechRecognition=window.SpeechRecognition||window.webkitSpeechRecognition;
  const style=document.createElement('style');
  style.textContent=`.mela-web-mic{position:fixed;right:16px;bottom:22px;z-index:9999;width:64px;height:64px;border:0;border-radius:50%;display:grid;place-items:center;background:radial-gradient(circle,#62ffd1,#08715d 60%,#053d34 62%);color:#fff;font-size:24px;font-weight:900;box-shadow:0 0 0 7px #5fffd329,0 13px 30px #002d24aa;touch-action:none}.mela-web-mic:before{content:"";position:absolute;inset:-9px;border:2px solid #60f5cd;border-left-color:transparent;border-radius:50%;animation:melaWebSpin 3s linear infinite}.mela-web-mic.listening{background:radial-gradient(circle,#fff176,#e04d72 62%,#70203b 64%)}@keyframes melaWebSpin{to{transform:rotate(1turn)}}.mela-web-toast{position:fixed;left:50%;bottom:98px;z-index:9999;transform:translateX(-50%);width:min(calc(100% - 28px),520px);padding:11px 14px;border-radius:13px;background:#052f29ed;color:#fff;text-align:center;font:700 13px/1.45 system-ui;box-shadow:0 10px 30px #0007;opacity:0;pointer-events:none;transition:.2s}.mela-web-toast.show{opacity:1}`;
  document.head.appendChild(style);
  const mic=document.createElement('button'),toast=document.createElement('div');
  mic.className='mela-web-mic';mic.type='button';mic.setAttribute('aria-label','음성 명령');mic.textContent='◉';
  toast.className='mela-web-toast';toast.setAttribute('aria-live','polite');
  document.body.append(mic,toast);
  let recognition=null,drag=false,moved=false,startX=0,startY=0,baseX=0,baseY=0,timer;
  const say=(message)=>{
    toast.textContent=message;toast.classList.add('show');clearTimeout(timer);timer=setTimeout(()=>toast.classList.remove('show'),3200);
    try{speechSynthesis.cancel();const u=new SpeechSynthesisUtterance(message);u.lang='ko-KR';speechSynthesis.speak(u)}catch(e){}
  };
  const listen=()=>{
    if(!SpeechRecognition){say('이 브라우저는 음성 인식을 지원하지 않습니다. 크롬 또는 사랑방 앱에서 이용해 주세요.');return}
    try{
      recognition?.abort();recognition=new SpeechRecognition();recognition.lang='ko-KR';recognition.interimResults=false;recognition.maxAlternatives=1;
      recognition.onstart=()=>{mic.classList.add('listening');say('말씀해 주세요.')};
      recognition.onend=()=>mic.classList.remove('listening');
      recognition.onerror=e=>say(e.error==='not-allowed'?'마이크 권한을 허용해 주세요.':'음성을 듣지 못했습니다. 다시 말씀해 주세요.');
      recognition.onresult=e=>{
        const text=e.results[0][0].transcript||'';toast.textContent='“'+text+'”';toast.classList.add('show');
        const handled=window.__SARANGBANG_ROUTE_COMMAND__?.(text);
        if(typeof handled==='string')say(handled);
        else if(!handled)say('명령을 찾지 못했습니다. AI 상담, 오버뷰 영상, 제품 영상, 보상플랜, 비즈니스, 구독, 카카오톡, APK 중 하나를 말씀해 주세요.');
        else say('요청을 실행합니다.');
      };
      recognition.start();
    }catch(e){say('음성 인식을 다시 눌러 주세요.')}
  };
  window.__SARANGBANG_LISTEN__=listen;
  mic.addEventListener('pointerdown',e=>{drag=true;moved=false;startX=e.clientX;startY=e.clientY;baseX=mic.offsetLeft;baseY=mic.offsetTop;mic.setPointerCapture(e.pointerId)});
  mic.addEventListener('pointermove',e=>{if(!drag)return;const dx=e.clientX-startX,dy=e.clientY-startY;if(Math.abs(dx)+Math.abs(dy)>7)moved=true;const x=Math.max(6,Math.min(innerWidth-mic.offsetWidth-6,baseX+dx)),y=Math.max(6,Math.min(innerHeight-mic.offsetHeight-6,baseY+dy));mic.style.left=x+'px';mic.style.top=y+'px';mic.style.right='auto';mic.style.bottom='auto'});
  mic.addEventListener('pointerup',()=>{drag=false;if(!moved)listen()});
  addEventListener('click',e=>{const b=e.target.closest?.('[onclick*="__SARANGBANG_LISTEN__"]');if(!b)return;e.preventDefault();e.stopImmediatePropagation();listen()},true);
})();
