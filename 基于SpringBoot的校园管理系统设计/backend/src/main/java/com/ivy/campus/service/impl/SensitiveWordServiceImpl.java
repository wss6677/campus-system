package com.ivy.campus.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ivy.campus.common.BizException;
import com.ivy.campus.common.PageResult;
import com.ivy.campus.entity.SensitiveWord;
import com.ivy.campus.mapper.SensitiveWordMapper;
import com.ivy.campus.service.SensitiveWordService;
import com.ivy.campus.util.PageUtils;
import com.ivy.campus.util.SensitiveWordUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class SensitiveWordServiceImpl implements SensitiveWordService {

    private static final long CACHE_MILLIS = 60_000L;

    private static final String LEVEL_BLOCK = "BLOCK";

    private static final String LEVEL_WARN = "WARN";

    private final SensitiveWordMapper sensitiveWordMapper;

    private final AtomicLong cacheStamp = new AtomicLong(0L);

    private volatile List<SensitiveWord> cacheWords = new ArrayList<>();

    private volatile Pattern cachePattern;

    /** 应用启动后把数据库词库灌入静态工具，保证公告提交前可直接校验 */
    @PostConstruct
    public void refreshStaticLibrary() {
        try {
            List<String> blockWords = new ArrayList<>();
            List<String> warnWords = new ArrayList<>();
            for (SensitiveWord word : loadWords()) {
                if (LEVEL_BLOCK.equalsIgnoreCase(word.getLevel())) {
                    blockWords.add(word.getWord());
                } else {
                    warnWords.add(word.getWord());
                }
                if (word.getReplacement() != null) {
                    SensitiveWordUtil.putReplacement(word.getWord(), word.getReplacement());
                }
            }
            if (!blockWords.isEmpty() || !warnWords.isEmpty()) {
                SensitiveWordUtil.refresh(blockWords, warnWords);
            }
            log.info("敏感词静态词库装载完成 block={} warn={}", blockWords.size(), warnWords.size());
        } catch (Exception e) {
            log.warn("敏感词静态词库装载失败：{}", e.getMessage());
        }
    }

    @Override
    public PageResult<SensitiveWord> page(Integer pageNum, Integer pageSize, String keyword, String level) {
        IPage<SensitiveWord> page = sensitiveWordMapper.selectPage(PageUtils.of(pageNum, pageSize),
                Wrappers.<SensitiveWord>lambdaQuery()
                        .like(keyword != null && !keyword.isBlank(), SensitiveWord::getWord, keyword)
                        .eq(level != null && !level.isBlank(), SensitiveWord::getLevel, level)
                        .orderByDesc(SensitiveWord::getId));
        return PageResult.of(page);
    }

    @Override
    public SensitiveWord getById(Long id) {
        SensitiveWord word = sensitiveWordMapper.selectById(id);
        if (word == null) {
            throw new BizException("敏感词不存在");
        }
        return word;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(SensitiveWord entity) {
        if (entity.getWord() == null || entity.getWord().isBlank()) {
            throw new BizException("敏感词不能为空");
        }
        entity.setId(null);
        entity.setLevel(entity.getLevel() == null || entity.getLevel().isBlank() ? LEVEL_WARN : entity.getLevel());
        entity.setReplacement(entity.getReplacement() == null ? "***" : entity.getReplacement());
        entity.setStatus(entity.getStatus() == null ? 1 : entity.getStatus());
        entity.setCreateTime(LocalDateTime.now());
        sensitiveWordMapper.insert(entity);
        invalidateCache();
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SensitiveWord entity) {
        SensitiveWord exists = getById(id);
        entity.setId(exists.getId());
        entity.setCreateTime(exists.getCreateTime());
        sensitiveWordMapper.updateById(entity);
        invalidateCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        getById(id);
        sensitiveWordMapper.deleteById(id);
        invalidateCache();
    }

    @Override
    public List<SensitiveWord> listAll() {
        return loadWords();
    }

    @Override
    public String hit(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        Pattern pattern = loadPattern();
        if (pattern == null) {
            return null;
        }
        List<SensitiveWord> words = loadWords();
        for (SensitiveWord word : words) {
            if (word.getWord() == null || word.getWord().isBlank()) {
                continue;
            }
            if (content.contains(word.getWord())) {
                return word.getWord();
            }
        }
        return null;
    }

    @Override
    public String hitLevel(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        for (SensitiveWord word : loadWords()) {
            if (word.getWord() == null || word.getWord().isBlank()) {
                continue;
            }
            if (content.contains(word.getWord())) {
                return word.getLevel() == null ? LEVEL_WARN : word.getLevel();
            }
        }
        return null;
    }

    private void invalidateCache() {
        cacheStamp.set(0L);
        refreshStaticLibrary();
    }

    private List<SensitiveWord> loadWords() {
        long now = System.currentTimeMillis();
        if (now - cacheStamp.get() < CACHE_MILLIS && !cacheWords.isEmpty()) {
            return cacheWords;
        }
        List<SensitiveWord> words = sensitiveWordMapper.selectList(Wrappers.<SensitiveWord>lambdaQuery()
                .eq(SensitiveWord::getStatus, 1)
                .orderByAsc(SensitiveWord::getId));
        cacheWords = words == null ? new ArrayList<>() : words;
        cachePattern = buildPattern(cacheWords);
        cacheStamp.set(now);
        return cacheWords;
    }

    private Pattern loadPattern() {
        loadWords();
        return cachePattern;
    }

    private Pattern buildPattern(List<SensitiveWord> words) {
        if (words.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (SensitiveWord word : words) {
            if (word.getWord() == null || word.getWord().isBlank()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('|');
            }
            sb.append(Pattern.quote(word.getWord()));
        }
        if (sb.length() == 0) {
            return null;
        }
        return Pattern.compile(sb.toString());
    }
}
