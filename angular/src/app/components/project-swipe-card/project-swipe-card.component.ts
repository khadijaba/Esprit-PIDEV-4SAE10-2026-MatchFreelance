import {
  Component,
  Input,
  Output,
  EventEmitter,
  OnDestroy,
  OnChanges,
  SimpleChanges,
  signal,
  computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Project } from '../../models/project.model';

const SWIPE_THRESHOLD = 80;
const MAX_DRAG = 150;
const FLY_OFF_DISTANCE = 400;

@Component({
  selector: 'app-project-swipe-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './project-swipe-card.component.html',
  styleUrl: './project-swipe-card.component.css',
})
export class ProjectSwipeCardComponent implements OnDestroy, OnChanges {
  @Input({ required: true }) project!: Project;

  @Output() apply = new EventEmitter<Project>();
  @Output() skip = new EventEmitter<void>();

  private startX = 0;
  private isDragging = false;
  private cleanup: (() => void) | null = null;

  dragX = signal(0);
  isFlyingOff = signal(false);
  flyDirection = signal<'left' | 'right' | null>(null);

  applyOpacity = computed(() => {
    const x = this.dragX();
    if (x <= 0) return 0;
    return Math.min(1, x / SWIPE_THRESHOLD) * 0.9;
  });

  skipOpacity = computed(() => {
    const x = this.dragX();
    if (x >= 0) return 0;
    return Math.min(1, Math.abs(x) / SWIPE_THRESHOLD) * 0.9;
  });

  cardStyle = computed(() => {
    const x = this.dragX();
    const flying = this.isFlyingOff();
    const dir = this.flyDirection();
    let translateX = x;
    let opacity = 1;
    if (flying && dir) {
      translateX = dir === 'right' ? FLY_OFF_DISTANCE : -FLY_OFF_DISTANCE;
      opacity = 0;
    }
    return {
      transform: `translateX(${translateX}px)`,
      opacity,
      transition: flying ? 'transform 0.3s ease-out, opacity 0.3s ease-out' : 'none',
    };
  });

  ngOnChanges(changes: SimpleChanges) {
    if (changes['project'] && !changes['project'].firstChange) {
      this.resetCardState();
    }
  }

  private resetCardState() {
    this.dragX.set(0);
    this.isFlyingOff.set(false);
    this.flyDirection.set(null);
  }

  onMouseDown(e: MouseEvent) {
    e.preventDefault();
    this.startDrag(e.clientX);
  }

  onTouchStart(e: TouchEvent) {
    if (e.touches.length > 0) {
      this.startDrag(e.touches[0].clientX);
    }
  }

  private startDrag(clientX: number) {
    this.startX = clientX;
    this.isDragging = true;
    const touchOpts: AddEventListenerOptions = { passive: false };
    this.cleanup = () => {
      window.removeEventListener('mousemove', this.boundMouseMove);
      window.removeEventListener('mouseup', this.boundMouseUp);
      window.removeEventListener('touchmove', this.boundTouchMove as EventListener, touchOpts);
      window.removeEventListener('touchend', this.boundTouchEnd);
      this.cleanup = null;
    };
    window.addEventListener('mousemove', this.boundMouseMove);
    window.addEventListener('mouseup', this.boundMouseUp);
    window.addEventListener('touchmove', this.boundTouchMove as EventListener, touchOpts);
    window.addEventListener('touchend', this.boundTouchEnd);
  }

  private boundMouseMove = (e: MouseEvent) => this.onDragMove(e.clientX);
  private boundMouseUp = () => this.onDragEnd();
  private boundTouchMove = (e: TouchEvent) => {
    e.preventDefault();
    if (e.touches.length > 0) this.onDragMove(e.touches[0].clientX);
  };
  private boundTouchEnd = () => this.onDragEnd();

  private onDragMove(clientX: number) {
    if (!this.isDragging) return;
    let delta = clientX - this.startX;
    delta = Math.max(-MAX_DRAG, Math.min(MAX_DRAG, delta));
    this.dragX.set(delta);
  }

  private onDragEnd() {
    if (!this.isDragging) return;
    this.isDragging = false;
    this.cleanup?.();

    const x = this.dragX();
    if (Math.abs(x) > SWIPE_THRESHOLD) {
      this.isFlyingOff.set(true);
      this.flyDirection.set(x > 0 ? 'right' : 'left');
      setTimeout(() => {
        if (x > 0) {
          this.apply.emit(this.project);
        } else {
          this.skip.emit();
        }
      }, 300);
    } else {
      this.dragX.set(0);
    }
  }

  onApplyClick() {
    if (this.isFlyingOff()) return;
    this.isFlyingOff.set(true);
    this.flyDirection.set('right');
    this.dragX.set(FLY_OFF_DISTANCE);
    setTimeout(() => this.apply.emit(this.project), 300);
  }

  onSkipClick() {
    if (this.isFlyingOff()) return;
    this.isFlyingOff.set(true);
    this.flyDirection.set('left');
    this.dragX.set(-FLY_OFF_DISTANCE);
    setTimeout(() => this.skip.emit(), 300);
  }

  ngOnDestroy() {
    this.cleanup?.();
  }
}
