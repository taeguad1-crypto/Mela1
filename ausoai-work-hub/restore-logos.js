/* R2: isolated icon pixels only; no phone screenshots or account information. */
(()=>{'use strict';
const base=new URL('.',document.currentScript.src),size=40,cols=10;
const ids=['health','namgu','naru','nareul','vitda','melaleuca','utility','legacy','ninja','ownerform','phone','sms','contacts','camera','photos','files','alarm','calc','naver','google','youtube','instagram','facebook','translate','papago','gmaps','gkeep','gphotos','gmeet','coupang','kakao','onedrive','suno','line','googleone','elevenlabs','notebook','melashopping','capcut','kakaonavi','gemini','clova','netlify','naverpay','navercalendar','mybox','canva','gov24','word','polaris','zoom','webex','whatsapp','ytstudio','outlook','super-sound','srt','cgv','playconsole','grow','chatgpt','sinternet','snotes'];
window.HUB_LOGO_READY=(async()=>{
 const chunks=await Promise.all(['logos-r2-a.txt','logos-r2-b.txt'].map(async file=>{const r=await fetch(new URL(file,base));if(!r.ok)throw Error('logo HTTP '+r.status);return r.text()}));
 const b64=chunks.join('').replace(/\s/g,'');if(b64.length!==31468)throw Error('logo length mismatch');
 if(globalThis.crypto?.subtle){const hash=Array.from(new Uint8Array(await crypto.subtle.digest('SHA-256',new TextEncoder().encode(b64)))).map(v=>v.toString(16).padStart(2,'0')).join('');if(hash!=='d5806354a3548db6cad8e4e1cf6899dabfc0a99797a442c8796b94f46481a0b8')throw Error('logo integrity mismatch')}
 const image=new Image();await new Promise((resolve,reject)=>{image.onload=resolve;image.onerror=()=>reject(Error('logo image decoding failed'));image.src='data:image/webp;base64,'+b64});
 if(image.width!==400||image.height!==280)throw Error('logo dimensions mismatch');
 const canvas=document.createElement('canvas');canvas.width=canvas.height=size;const ctx=canvas.getContext('2d');if(!ctx)throw Error('canvas unavailable');
 const assets=window.HUB_ASSETS||(window.HUB_ASSETS={});ids.forEach((id,i)=>{ctx.clearRect(0,0,size,size);ctx.drawImage(image,(i%cols)*size,Math.floor(i/cols)*size,size,size,0,0,size,size);assets[id]=canvas.toDataURL('image/png')});
 window.HUB_LOGOS_LOADED=ids.length;
 const sections=document.querySelector('.shell[data-restored] #sections');if(sections)sections.replaceChildren();
 return ids.length;
})().catch(error=>{console.error('AUSOAI original logo loading failed:',error);window.HUB_LOGOS_ERROR=String(error);const note=document.createElement('p');note.textContent='일부 원본 로고를 불러오지 못했습니다. 새로고침해 주세요.';note.style.cssText='font:11px system-ui;color:#175b59;text-align:center;padding:8px';(document.querySelector('.shell')||document.body).append(note);return 0});
})();
