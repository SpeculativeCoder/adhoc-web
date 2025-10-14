/*
 * Copyright (c) 2022-2025 SpeculativeCoder (https://github.com/SpeculativeCoder)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package adhoc.message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;

    @Transactional(readOnly = true)
    public Page<MessageDto> getMessages(Optional<Long> optionalUserId, Pageable pageable) {
        // TODO
        return messageRepository.findByUserNullOrUserId(optionalUserId.orElse(null), pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public MessageDto getMessage(Long id, Optional<Long> optionalUserId) {
        // TODO
        return toDto(messageRepository.findByIdAnd_UserNullOrUserId_(id, optionalUserId.orElse(null)));
    }

    public void addGlobalMessage(String text) {
        LocalDateTime now = LocalDateTime.now();

        MessageEntity message = new MessageEntity();
        message.setTimestamp(now);
        message.setText(text);

        messageRepository.save(message);
    }

    MessageDto toDto(MessageEntity message) {
        return new MessageDto(
                message.getId(),
                message.getVersion(),
                message.getText(),
                message.getTimestamp(),
                message.getUser() == null ? null : message.getUser().getId());
    }
}
