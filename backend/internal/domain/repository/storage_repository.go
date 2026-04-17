package repository

import (
	"context"
	"io"
)

type StorageRepository interface {
	UploadFile(ctx context.Context, fileName string, contentType string, file io.Reader) (string, error)
}
