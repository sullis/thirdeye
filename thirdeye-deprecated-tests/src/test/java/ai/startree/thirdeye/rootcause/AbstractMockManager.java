/*
 * Copyright (c) 2022 StarTree Inc. All rights reserved.
 * Confidential and Proprietary Information of StarTree Inc.
 */

package ai.startree.thirdeye.rootcause;

import ai.startree.thirdeye.spi.datalayer.DaoFilter;
import ai.startree.thirdeye.spi.datalayer.Predicate;
import ai.startree.thirdeye.spi.datalayer.bao.AbstractManager;
import ai.startree.thirdeye.spi.datalayer.dto.AbstractDTO;
import java.util.List;
import java.util.Map;

public abstract class AbstractMockManager<T extends AbstractDTO> implements AbstractManager<T> {

  @Override
  public Long save(T entity) {
    throw new AssertionError("not implemented");
  }

  @Override
  public int update(T entity) {
    throw new AssertionError("not implemented");
  }

  @Override
  public int update(List<T> entities) {
    throw new AssertionError("not implemented");
  }

  @Override
  public T findById(Long id) {
    throw new AssertionError("not implemented");
  }

  @Override
  public List<T> findByIds(List<Long> id) {
    throw new AssertionError("not implemented");
  }

  @Override
  public List<T> filter(final DaoFilter daoFilter) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int delete(T entity) {
    throw new AssertionError("not implemented");
  }

  @Override
  public int deleteById(Long id) {
    throw new AssertionError("not implemented");
  }

  @Override
  public int deleteByIds(List<Long> id) {
    throw new AssertionError("not implemented");
  }

  @Override
  public int deleteByPredicate(Predicate predicate) {
    throw new AssertionError("not implemented");
  }

  @Override
  public int deleteRecordsOlderThanDays(int days) {
    throw new AssertionError("not implemented");
  }

  @Override
  public List<T> findAll() {
    throw new AssertionError("not implemented");
  }

  @Override
  public List<T> findByParams(Map<String, Object> filters) {
    throw new AssertionError("not implemented");
  }

  @Override
  public List<T> findByPredicate(Predicate predicate) {
    throw new AssertionError("not implemented");
  }

  @Override
  public int update(T entity, Predicate predicate) {
    throw new AssertionError("not implemented");
  }
}