import { Pipe, PipeTransform } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

@Pipe({ name: 'markdown' })
export class MarkdownPipe implements PipeTransform {

  constructor(private sanitizer: DomSanitizer) {}

  transform(text: string): SafeHtml {
    if (!text) return '';

    // 1. Escape HTML to prevent XSS
    let html = text
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;');

    // 2. Bold: **text**
    html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');

    // 3. Markdown links: [text](url)
    html = html.replace(
      /\[([^\]]+)\]\((https?:\/\/[^\s)]+)\)/g,
      '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>'
    );

    // 4. Plain URLs that are not inside an <a> tag already
    html = html.replace(
      /(?<!href=")(https?:\/\/[^\s<"]+)/g,
      '<a href="$1" target="_blank" rel="noopener noreferrer">$1</a>'
    );

    // 5. Process lines: group bullet lines into <ul>, join others with <br>
    const lines = html.split('\n');
    let result = '';
    let inList = false;

    for (let i = 0; i < lines.length; i++) {
      const line = lines[i];
      const isBullet = /^[-•]\s/.test(line);

      if (isBullet) {
        if (!inList) {
          if (result) result += '<br>';
          result += '<ul class="chat-list">';
          inList = true;
        }
        result += `<li>${line.replace(/^[-•]\s/, '')}</li>`;
      } else {
        if (inList) {
          result += '</ul>';
          inList = false;
          if (line.trim()) result += '<br>';
        } else if (i > 0) {
          result += '<br>';
        }
        result += line;
      }
    }

    if (inList) result += '</ul>';

    return this.sanitizer.bypassSecurityTrustHtml(result);
  }
}
